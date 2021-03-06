package im.zego.commonlibs.whiteboard

import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import im.zego.zegodocs.*
import im.zego.zegowhiteboard.ZegoWhiteboardManager
import im.zego.zegowhiteboard.ZegoWhiteboardView
import im.zego.zegowhiteboard.ZegoWhiteboardViewImageFitMode
import im.zego.zegowhiteboard.ZegoWhiteboardViewImageType
import im.zego.zegowhiteboard.callback.IZegoWhiteboardExecuteListener
import im.zego.zegowhiteboard.callback.IZegoWhiteboardViewScaleListener
import im.zego.zegowhiteboard.callback.IZegoWhiteboardViewScrollListener
import im.zego.zegowhiteboard.model.ZegoWhiteboardViewModel
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil
import kotlin.math.round

/**
 * 白板容器, 一个文件对应一个容器，包含 docsview 和白板
 * 如果是 excel，一个 docsView 可能会对应多个白板
 * 其他类型的文件，一个 docsView 只有一个白板
 */
class ZegoWhiteboardViewHolder : FrameLayout {
    val TAG = "WhiteboardViewHolder"

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var whiteboardViewList: MutableList<ZegoWhiteboardView> = mutableListOf()
    private var zegoDocsView: ZegoDocsView? = null
    private var fileLoadSuccessed = false
    private var whiteboardViewAddFinished = false
    var currentWhiteboardSize: Size = Size(0, 0)

    // 动态 PPT 是转成 H5 进行加载的，首次会加载第 1 页第 1 步，此时不需要同步给其他端的。通过这个字段来过滤
    private var firstFlipPage = true

    private var internalScrollListener: IZegoWhiteboardViewScrollListener =
        IZegoWhiteboardViewScrollListener { horizontalPercent, verticalPercent ->
            outScrollListener?.onScroll(horizontalPercent, verticalPercent)
        }
    private var outScrollListener: IZegoWhiteboardViewScrollListener? = null

    /**
     * 当前显示的白板ID
     */
    var currentWhiteboardID = 0L
        set(value) {
            field = value
            Log.d(TAG, "set currentWhiteboardID:${value}")
            var selectedView: ZegoWhiteboardView? = null
            whiteboardViewList.forEach {
                val viewModel = it.getWhiteboardViewModel()
                if (viewModel.whiteboardID == value) {
                    it.visibility = View.VISIBLE
                    selectedView = it
                } else {
                    it.visibility = View.GONE
                }
                Log.d(
                    TAG,
                    "whiteboardViewList: ${viewModel.whiteboardID}:${viewModel.fileInfo.fileName}"
                )
            }
            selectedView?.let {
                Log.d(TAG, "selectedView:${it.whiteboardViewModel.fileInfo.fileName}")
                val viewModel = it.whiteboardViewModel
                if (zegoDocsView != null && isExcel()) {
                    val fileName = viewModel.fileInfo.fileName
                    val sheetIndex = getExcelSheetNameList().indexOf(fileName)
                    zegoDocsView!!.switchSheet(sheetIndex, IZegoDocsViewLoadListener { loadResult ->
                        Log.d(TAG, "loadResult = $loadResult")
                        if (loadResult == 0) {
                            Log.i(
                                TAG, "switchSheet,sheetIndex:$sheetIndex," +
                                        "visibleSize:${zegoDocsView!!.getVisibleSize()}" +
                                        "contentSize:${zegoDocsView!!.getContentSize()}"
                            )
                            viewModel.aspectWidth = zegoDocsView!!.getContentSize().width
                            viewModel.aspectHeight = zegoDocsView!!.getContentSize().height

                            connectDocsViewAndWhiteboardView(it)

                            zegoDocsView!!.scaleDocsView(
                                it.getScaleFactor(),
                                it.getScaleOffsetX(),
                                it.getScaleOffsetY()
                            )
                        }
                    })
                }
            }
        }

    fun setDocsScaleEnable(selected: Boolean) {
        zegoDocsView?.isScaleEnable = selected
    }

    private var currentWhiteboardView: ZegoWhiteboardView?
        private set(value) {}
        get() {
            return whiteboardViewList.firstOrNull {
                it.whiteboardViewModel.whiteboardID == currentWhiteboardID
            }
        }

    fun hasWhiteboardID(whiteboardID: Long): Boolean {
        val firstOrNull = whiteboardViewList.firstOrNull {
            it.whiteboardViewModel.whiteboardID == whiteboardID
        }
        return firstOrNull != null
    }

    fun getWhiteboardIDList(): List<Long> {
        return whiteboardViewList.map { it.whiteboardViewModel.whiteboardID }
    }

    fun isFileWhiteboard(): Boolean {
        return getFileID() != null
    }

    fun isPureWhiteboard(): Boolean {
        return getFileID() == null
    }

    fun getFileID(): String? {
        return when {
            zegoDocsView != null -> {
                zegoDocsView!!.getFileID()
            }
            whiteboardViewList.isNotEmpty() -> {
                val fileInfo = whiteboardViewList.first().getWhiteboardViewModel().fileInfo
                if (fileInfo.fileID.isEmpty()) return null else fileInfo.fileID
            }
            else -> {
                null
            }
        }
    }

    fun getExcelSheetNameList(): MutableList<String> {
        return if (isExcel() && isDocsViewLoadSuccessed()) {
            zegoDocsView!!.sheetNameList
        } else {
            mutableListOf()
        }
    }

    fun selectExcelSheet(sheetIndex: Int, selectResult: (String, Long) -> Unit) {
        if (sheetIndex < 0 || sheetIndex > getExcelSheetNameList().size - 1) {
            return
        }
        if (isExcel() && isDocsViewLoadSuccessed()) {
            val firstOrNull = whiteboardViewList.firstOrNull {
                it.whiteboardViewModel.fileInfo.fileName == getExcelSheetNameList()[sheetIndex]
            }
            firstOrNull?.let {
                val model = it.whiteboardViewModel
                Log.i(
                    TAG,
                    "selectSheet,fileName：${model.fileInfo.fileName}，${model.whiteboardID}"
                )
                currentWhiteboardID = model.whiteboardID
                selectResult(model.fileInfo.fileName, model.whiteboardID)
            }
        }
    }

    fun isExcel(): Boolean {
        return getFileType() == ZegoDocsViewConstants.ZegoDocsViewFileTypeELS
    }

    fun isDisplayedByWebView(): Boolean {
        return getFileType() == ZegoDocsViewConstants.ZegoDocsViewFileTypeDynamicPPTH5 ||
                getFileType() == ZegoDocsViewConstants.ZegoDocsViewFileTypeCustomH5
    }

    fun isPPT(): Boolean {
        return getFileType() == ZegoDocsViewConstants.ZegoDocsViewFileTypePPT
    }

    fun getThumbnailUrlList(): ArrayList<String> {
        val urls = ArrayList<String>()
        if (zegoDocsView != null) {
            return zegoDocsView!!.getThumbnailUrlList()
        }
        return urls
    }


    fun getFileType(): Int {
        return when {
            zegoDocsView != null && isDocsViewLoadSuccessed() -> {
                zegoDocsView!!.getFileType()
            }
            whiteboardViewList.isNotEmpty() -> {
                // 任意一个白板，包含的是同样的 fileInfo
                whiteboardViewList.first().getWhiteboardViewModel().fileInfo.fileType
            }
            else -> {
                ZegoDocsViewConstants.ZegoDocsViewFileTypeUnknown
            }
        }
    }

    fun supportDragWhiteboard(): Boolean {
        return !(isPureWhiteboard() || isDisplayedByWebView() || isPPT())
    }

    fun getCurrentWhiteboardName(): String? {
        return getCurrentWhiteboardModel().name
    }

    fun getCurrentWhiteboardModel(): ZegoWhiteboardViewModel {
        return currentWhiteboardView!!.whiteboardViewModel
    }

    fun getCurrentWhiteboardMsg(): String {
        return "modelMessage:name:${getCurrentWhiteboardModel().name},whiteboardID:${getCurrentWhiteboardModel().whiteboardID}," +
                "fileInfo:${getCurrentWhiteboardModel().fileInfo.fileName}" +
                "hori:${getCurrentWhiteboardModel().horizontalScrollPercent},vertical:${getCurrentWhiteboardModel().verticalScrollPercent}"
    }

    fun addTextEdit(listener:IZegoWhiteboardExecuteListener) {
        currentWhiteboardView?.addTextEdit(listener)
    }

    fun undo() {
        currentWhiteboardView?.undo()
    }

    fun redo() {
        currentWhiteboardView?.redo()
    }

    fun clearCurrentPage(listener:IZegoWhiteboardExecuteListener) {
        val curPageRectF = if (isPureWhiteboard()) {
            currentWhiteboardView?.let {
                val width = it.width.toFloat()
                val height = it.height.toFloat()
                val pageOffsetX = width * (getCurrentPage() - 1)
                val pageOffsetY = 0F

                RectF(
                    pageOffsetX,
                    pageOffsetY,
                    (pageOffsetX + width),
                    (pageOffsetY + height)
                )
            }

        } else {
            zegoDocsView!!.currentPageInfo!!.rect
        }

        Log.i(TAG, "clearCurrentPage: ${curPageRectF.toString()}")
        currentWhiteboardView?.clear(curPageRectF!!,listener)
    }

    fun setOperationMode(opMode:Int) {
        currentWhiteboardView?.setWhiteboardOperationMode(opMode)
    }

    fun scrollTo(horizontalPercent: Float, verticalPercent: Float, currentStep: Int = 1,listener: IZegoWhiteboardExecuteListener?) {
        Log.d(
            TAG,
            "scrollTo() called with: horizontalPercent = $horizontalPercent, verticalPercent = $verticalPercent, currentStep = $currentStep"
        )
        if (getFileID() != null) {
            if (isDocsViewLoadSuccessed()) {
                currentWhiteboardView?.scrollTo(horizontalPercent, verticalPercent, currentStep,listener)
            }
        } else {
            currentWhiteboardView?.scrollTo(horizontalPercent, verticalPercent, currentStep,listener)
        }
    }

    fun clear(listener:IZegoWhiteboardExecuteListener) {
        currentWhiteboardView?.clear(listener)
    }

    private fun addDocsView(docsView: ZegoDocsView, estimatedSize: Size) {
        Log.d(TAG, "addDocsView, estimatedSize:$estimatedSize")
        docsView.setEstimatedSize(estimatedSize.width, estimatedSize.height)
        this.zegoDocsView = docsView
        addView(
            zegoDocsView, 0, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun addWhiteboardView(zegoWhiteboardView: ZegoWhiteboardView) {
        val model = zegoWhiteboardView.whiteboardViewModel
        Log.i(
            TAG, "addWhiteboardView:${model.whiteboardID},${model.name},${model.fileInfo.fileName}"
        )
        this.whiteboardViewList.add(zegoWhiteboardView)

        addView(
            zegoWhiteboardView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).also {
                it.gravity = Gravity.CENTER
            }
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "onSizeChanged() called with: w = $w, h = $h, oldw = $oldw, oldh = $oldh")
        reload(Size(w, h))
    }

    private fun connectDocsViewAndWhiteboardView(zegoWhiteboardView: ZegoWhiteboardView) {
        Log.i(TAG, "connectDocsViewAndWhiteboardView..., currentWhiteboardID=$currentWhiteboardID")
        zegoDocsView?.let { docsview ->
            if (docsview.getVisibleSize().height != 0 || docsview.getVisibleSize().width != 0) {
                zegoWhiteboardView.setVisibleRegion(zegoDocsView!!.getVisibleSize())
            }
            zegoWhiteboardView.setScrollListener { horizontalPercent, verticalPercent ->
                Log.d(
                    TAG,
                    "ScrollListener.onScroll,horizontalPercent:${horizontalPercent},verticalPercent:${verticalPercent}"
                )
                if (isDisplayedByWebView()) {
                    val page = calcDynamicPPTPage(verticalPercent)
                    val model = zegoWhiteboardView.whiteboardViewModel
                    val stepChanged = docsview.currentStep != model.pptStep
                    val pageChanged = docsview.currentPage != page
                    Log.i(
                        TAG,
                        "page:${page},step:${model.pptStep},stepChanged:$stepChanged,pageChanged:$pageChanged,currentWhiteboardID=$currentWhiteboardID"
                    )
                    docsview.flipPage(page, model.pptStep) { result ->
                        Log.i(TAG, "docsview.flipPage() : result = $result")
                    }
                    internalScrollListener.onScroll(horizontalPercent, verticalPercent)
                } else {
                    docsview.scrollTo(verticalPercent) { complete ->
                        Log.d(
                            TAG,
                            "connectDocsViewAndWhiteboardView() called with: complete = $complete,docsview:${docsview.verticalPercent}"
                        )
                        internalScrollListener.onScroll(0f, verticalPercent)
                    }
                }

            }

            if (isDisplayedByWebView()) {
                // 这些需要在第一页加载出来才能
                // 对于动态PPT，H5可以自行播放动画，需要同步给白板，再同步给其他端的用户
                docsview.setAnimationListener(IZegoDocsViewAnimationListener {
                    if (windowVisibility == View.VISIBLE) {
                        zegoWhiteboardView.playAnimation(it)
                    }
                })

                docsview.setStepChangeListener(object : IZegoDocsViewCurrentStepChangeListener {
                    override fun onChanged() {
                    }

                    override fun onStepChangeForClick() {
                        // 动态PPT，直接点击H5，触发翻页、步数变化
                        Log.d(TAG, "onStepChangeForClick() called，scroll to ${docsview.verticalPercent}")
                        scrollTo(0f, docsview.verticalPercent, docsview.currentStep){
                        }
                    }
                })
            }
            // 对于动态PPT，其他端有播放动画，需要同步给docsView进行播放动画
            zegoWhiteboardView.setAnimationListener { animation ->
                Log.d(TAG, "setAnimationListener() called")
                docsview.playAnimation(animation)
            }
            zegoWhiteboardView.setScaleListener(IZegoWhiteboardViewScaleListener { scaleFactor, transX, transY ->
//            Log.d(TAG,"scaleFactor:$scaleFactor,transX:$transX,transY:$transY")
                docsview.scaleDocsView(scaleFactor, transX, transY)
            })
        }

        post {
            val model = zegoWhiteboardView.whiteboardViewModel
            val horPercent = model.horizontalScrollPercent
            val verPercent = model.verticalScrollPercent
            val currentStep = model.pptStep
            Log.d(TAG, "horPercent:$horPercent,verPercent:$verPercent,currentStep:$currentStep")
            if (isDisplayedByWebView()) {
                // 此处是首次加载，要跳转到到文件对应页。完成后需要判断是否播动画
                zegoDocsView?.let {
                    val targetPage = calcDynamicPPTPage(verPercent)
                    it.flipPage(targetPage, currentStep) { result ->
                        if (result) {
                            zegoWhiteboardView.whiteboardViewModel.h5Extra?.let { h5Extra ->
                                it.playAnimation(h5Extra)
                            }
                        }
                    }
                }
            } else {
                zegoDocsView?.scrollTo(verPercent){complete ->
                    Log.d(
                            TAG,
                            "connectDocsViewAndWhiteboardView() called with: complete = $complete,docsview:${zegoDocsView?.verticalPercent}"
                    )
                    internalScrollListener.onScroll(0f, verPercent)
                }

            }
        }
    }

    fun calcDynamicPPTPage(verticalPercent: Float): Int {
        return if (isDisplayedByWebView()) {
            if (isDocsViewLoadSuccessed()) {
                val page = round(verticalPercent * zegoDocsView!!.pageCount).toInt() + 1
                page
            } else {
                1
            }
        } else {
            throw IllegalArgumentException("only used for dynamic PPT")
        }
    }

    private fun onPureWhiteboardViewAdded(zegoWhiteboardView: ZegoWhiteboardView) {
        val model = zegoWhiteboardView.getWhiteboardViewModel()
        currentWhiteboardID = model.whiteboardID
        zegoWhiteboardView.setScrollListener(IZegoWhiteboardViewScrollListener { horizontalPercent, verticalPercent ->
            internalScrollListener.onScroll(horizontalPercent, verticalPercent)
        })
    }

    /**
     * 添加纯白板
     */
    fun onReceivePureWhiteboardView(zegoWhiteboardView: ZegoWhiteboardView) {
        zegoWhiteboardView.getChildAt(0)?.setBackgroundColor(Color.WHITE)
        zegoWhiteboardView.setBackgroundColor(Color.BLACK)
        addWhiteboardView(zegoWhiteboardView)
        onPureWhiteboardViewAdded(zegoWhiteboardView)
        whiteboardViewAddFinished = true
    }

    /**
     * 创建纯白板，aspectWidth，aspectHeight:宽高比
     */
    fun createPureWhiteboardView(
        aspectWidth: Int, aspectHeight: Int, pageCount: Int,
        whiteboardName: String, requestResult: (Int) -> Unit
    ) {
        val data = ZegoWhiteboardViewModel()
        data.aspectHeight = aspectHeight
        data.aspectWidth = aspectWidth
        data.name = whiteboardName
        data.pageCount = pageCount
        ZegoWhiteboardManager.getInstance().createWhiteboardView(data)
        { errorCode, zegoWhiteboardView ->
            Log.d(
                TAG,
                "createPureWhiteboardView,name:${data.name},errorCode:${errorCode}"
            )
            if (errorCode == 0 && zegoWhiteboardView != null) {
                onReceivePureWhiteboardView(zegoWhiteboardView)
            } else {
                Toast.makeText(context, "创建白板失败，错误码:$errorCode", Toast.LENGTH_LONG).show()
            }
            requestResult.invoke(errorCode)
        }
    }

    fun destroyWhiteboardView(requestResult: (Int) -> Unit) {
        if (isExcel()) {
            var count = whiteboardViewList.size
            var success = true
            var code = 0
            whiteboardViewList.forEach {
                val whiteboardID = it.getWhiteboardViewModel().whiteboardID
                ZegoWhiteboardManager.getInstance().destroyWhiteboardView(whiteboardID)
                { errorCode, _ ->
                    //因为所有的回调都是在主线程，所以不用考虑多线程
                    count--
                    if (errorCode != 0) {
                        success = false
                        code = errorCode
                    }
                    if (count == 0) {
                        if (!success) {
                            Toast.makeText(context, "删除白板失败:错误码:$code", Toast.LENGTH_LONG)
                                .show()
                        } else {
                            unloadFile()
                            fileLoadSuccessed = false
                        }
                        requestResult.invoke(errorCode)
                    }
                }
            }
        } else {
            ZegoWhiteboardManager.getInstance().destroyWhiteboardView(currentWhiteboardID)
            { errorCode, _ ->
                if (errorCode != 0) {
                    Toast.makeText(context, "删除白板失败:错误码:$errorCode", Toast.LENGTH_LONG).show()
                } else {
                    unloadFile()
                    fileLoadSuccessed = false
                }
                requestResult.invoke(errorCode)
            }
        }
    }

    /**
     * 收到文件白板
     */
    fun onReceiveFileWhiteboard(
        estimatedSize: Size,
        zegoWhiteboardView: ZegoWhiteboardView,
        processResult: (Int, ZegoWhiteboardViewHolder) -> Unit
    ) {
        val fileInfo = zegoWhiteboardView.whiteboardViewModel.fileInfo
        Log.d(
            TAG,
            "onReceiveFileWhiteboard() called with: estimatedSize = $estimatedSize, zegoWhiteboardView = ${fileInfo.fileName}"
        )
        addWhiteboardView(zegoWhiteboardView)
        if (zegoDocsView != null) {
            zegoWhiteboardView.visibility = View.GONE
            processResult(0, this)
        } else {
            val fileID = fileInfo.fileID
            visibility = View.GONE
            currentWhiteboardID = zegoWhiteboardView.whiteboardViewModel.whiteboardID
            loadFileWhiteBoardView(fileID, estimatedSize) { errorCode: Int, _: ZegoDocsView ->
                if (errorCode == 0) {
                    // excel要等到load完才设置，因为要 switchSheet
                    if (isExcel()) {
                        currentWhiteboardID =
                            zegoWhiteboardView.getWhiteboardViewModel().whiteboardID
                    } else {
                        connectDocsViewAndWhiteboardView(zegoWhiteboardView)
                    }
                    processResult.invoke(errorCode, this)
                } else {
                    Toast.makeText(context, "加载文件失败，错误代码 $errorCode", Toast.LENGTH_LONG).show()
                    processResult.invoke(errorCode, this)
                }
            }
        }
    }

    /**
     * 加载白板view
     */
    private fun loadFileWhiteBoardView(
        fileID: String,
        estimatedSize: Size,
        requestResult: (Int, ZegoDocsView) -> Unit
    ) {
        Log.i(
            TAG,
            "loadFileWhiteBoardView,start loadFile fileID:${fileID},estimatedSize:${estimatedSize}"
        )
        val docsView = ZegoDocsView(context)
        docsView.setBackgroundColor(Color.BLACK)
        addDocsView(docsView, estimatedSize)
        loadDocsFile(fileID) { errorCode ->
            requestResult.invoke(errorCode, docsView)
        }
    }

    fun createDocsAndWhiteBoardView(
        fileID: String, estimatedSize: Size, createResult: (Int) -> Unit
    ) {
        loadFileWhiteBoardView(fileID, estimatedSize)
        { errorCode, docsView ->
            if (errorCode == 0) {
                if (isExcel()) {
                    createExcelWhiteboardViewList(docsView, createResult)
                } else {
                    createWhiteBoardViewInner(docsView, 0, createResult)
                }
            } else {
                createResult(errorCode)
                Toast.makeText(context, "加载文件失败，错误代码 $errorCode", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun isDocsViewLoadSuccessed(): Boolean {
        return fileLoadSuccessed
    }

    fun isWhiteboardViewAddFinished(): Boolean {
        return whiteboardViewAddFinished
    }

    private fun createExcelWhiteboardViewList(
        docsView: ZegoDocsView,
        requestResult: (Int) -> Unit
    ) {
        val sheetCount = getExcelSheetNameList().size
        var processCount = 0
        var resultCode = 0
        for (index in 0 until sheetCount) {
            createWhiteBoardViewInner(docsView, index) { code ->
                if (code != 0) {
                    resultCode = code
                }
                processCount++
                if (processCount == sheetCount) {
                    selectExcelSheet(0) { _, _ ->
                        whiteboardViewAddFinished = true
                        requestResult.invoke(resultCode)
                    }
                }
            }
        }
    }

    private fun createWhiteBoardViewInner(
        docsView: ZegoDocsView, index: Int,
        requestResult: (Int) -> Unit
    ) {
        val data = ZegoWhiteboardViewModel()
        data.aspectWidth = docsView.contentSize.width
        data.aspectHeight = docsView.contentSize.height
        data.name = docsView.fileName!!
        data.pageCount = docsView.pageCount
        data.fileInfo.fileID = docsView.fileID!!
        data.fileInfo.fileType = docsView.fileType
        if (isExcel()) {
            data.fileInfo.fileName = docsView.sheetNameList[index]
        }

        ZegoWhiteboardManager.getInstance().createWhiteboardView(data)
        { errorCode, zegoWhiteboardView ->
            Log.d(
                TAG,
                "createWhiteboardView,name:${data.name},fileName:${data.fileInfo.fileName}"
            )
            if (errorCode == 0 && zegoWhiteboardView != null) {
                addWhiteboardView(zegoWhiteboardView)
                if (!isExcel()) {
                    currentWhiteboardID =
                        zegoWhiteboardView.getWhiteboardViewModel().whiteboardID
                    connectDocsViewAndWhiteboardView(zegoWhiteboardView)
                    whiteboardViewAddFinished = errorCode == 0
                }
            } else {
                Toast.makeText(
                    context,
                    "创建白板失败，错误码:$errorCode",
                    Toast.LENGTH_LONG
                ).show()
            }
            requestResult.invoke(errorCode)
        }
    }

    fun flipToPage(targetPage: Int,listener: IZegoWhiteboardExecuteListener?) {
        Log.i(TAG, "targetPage:${targetPage}")
        if (zegoDocsView != null && getFileID() != null && isDocsViewLoadSuccessed()) {
            zegoDocsView!!.flipPage(targetPage) { result ->
                Log.i(TAG, "it.flipToPage() result:$result")
                if (result) {
                    scrollTo(0f, zegoDocsView!!.getVerticalPercent(),1,listener )
                }
            }
        } else {
            scrollTo((targetPage - 1).toFloat() / getPageCount(), 0f,1,listener)
        }
    }

    /**
     * 此处的page是从1开始的
     */
    fun flipToPrevPage(listener: IZegoWhiteboardExecuteListener?): Int {
        val currentPage = getCurrentPage()
        val targetPage = if (currentPage - 1 <= 0) 1 else currentPage - 1
        if (targetPage != currentPage) {
            flipToPage(targetPage,listener)
        }
        return targetPage
    }

    fun flipToNextPage(listener: IZegoWhiteboardExecuteListener?): Int {
        val currentPage = getCurrentPage()
        val targetPage =
            if (currentPage + 1 > getPageCount()) getPageCount() else currentPage + 1
        if (targetPage != currentPage) {
            flipToPage(targetPage,listener)
        }
        return targetPage
    }

    fun previousStep(listener: IZegoWhiteboardExecuteListener?) {
        Log.d(TAG, "previousStep() called,fileLoadSuccessed:${isDocsViewLoadSuccessed()}")
        if (getFileID() != null && isDisplayedByWebView() && isDocsViewLoadSuccessed()) {
            zegoDocsView?.let {
                it.previousStep(IZegoDocsViewScrollCompleteListener { result ->
                    Log.d(TAG, "previousStep:result = $result")
                    if (result) {
                        scrollTo(0f, it.getVerticalPercent(), it.getCurrentStep(),listener)
                    }
                })
            }
        }
    }

    fun nextStep(listener: IZegoWhiteboardExecuteListener?) {
        Log.i(TAG, "nextStep() called,fileLoadSuccessed:${isDocsViewLoadSuccessed()}")
        if (getFileID() != null && isDisplayedByWebView() && isDocsViewLoadSuccessed()) {
            zegoDocsView?.let {
                it.nextStep(IZegoDocsViewScrollCompleteListener { result ->
                    Log.i(TAG, "nextStep:result = $result")
                    if (result) {
                        scrollTo(0f, it.getVerticalPercent(), it.getCurrentStep(),listener)
                    }
                })
            }
        }
    }

    fun getPageCount(): Int {
        return if (getFileID() != null) {
            zegoDocsView!!.getPageCount()
        } else {
            getCurrentWhiteboardModel().pageCount
        }
    }

    /**
     * 第二页滚动到一半，才认为是第二页
     */
    fun getCurrentPage(): Int {
        return if (getFileID() != null) {
            zegoDocsView!!.getCurrentPage()
        } else {
            val percent = currentWhiteboardView!!.getHorizontalPercent()
            val currentPage = round(percent * getPageCount()).toInt() + 1
            Log.i(TAG, "getCurrentPage,percent:${percent},currentPage:${currentPage}")
            return if (currentPage < getPageCount()) currentPage else getPageCount()
        }
    }

    fun setWhiteboardScrollChangeListener(listener: IZegoWhiteboardViewScrollListener) {
        outScrollListener = listener
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.i(TAG, "onDetachedFromWindow... currentWhiteboardID=$currentWhiteboardID")
        // 保底处理
        unloadFile()
        fileLoadSuccessed = false
    }

    fun addText(text: String, positionX: Int, positionY: Int, listener: IZegoWhiteboardExecuteListener) {
        currentWhiteboardView?.addText(text, positionX, positionY,listener)
    }

    fun addImage(
        type: ZegoWhiteboardViewImageType,
        address: String,
        positionX: Int,
        positionY: Int,
        addResult: (errorCode: Int) -> Unit
    ) {
        currentWhiteboardView?.addImage(
            type, positionX, positionY, address
        ) {
            addResult(it)
        }
    }

    fun setBackgroundImage(address :String,mode :ZegoWhiteboardViewImageFitMode,result: (errorCode: Int) -> Unit){
        currentWhiteboardView?.setBackgroundImage(address,mode){
            result(it)
        }
    }

    fun clearBackgroundImage(result: (errorCode: Int) -> Unit){
        currentWhiteboardView?.clearBackgroundImage{
            result(it)
        }
    }

    fun deleteSelectedGraphics(listener:IZegoWhiteboardExecuteListener) {
        currentWhiteboardView?.deleteSelectedGraphics(listener)
    }

    /**
     * 停止当前正在播放的视频
     */
    fun stopPlayPPTVideo() {
        if (isDisplayedByWebView()) {
            zegoDocsView?.let {
                it.stopPlay(it.currentPage)
            }
        }
    }

    /**
     * 加载文件
     * @param fileID 文件 ID
     */
    private fun loadDocsFile(fileID: String, listener: IZegoDocsViewLoadListener) {
        zegoDocsView?.loadFile(fileID, "", IZegoDocsViewLoadListener { errorCode: Int ->
            fileLoadSuccessed = errorCode == 0
            if (errorCode == 0) {
                Log.i(
                    TAG,
                    "loadDocsFile fileID:${fileID} success,getVisibleSize:${zegoDocsView!!.getVisibleSize()}," +
                            "contentSize:${zegoDocsView!!.getContentSize()}," + "name:${zegoDocsView!!.getFileName()}," +
                            "nameList:${zegoDocsView!!.getSheetNameList()}"
                )
//                zegoDocsView!!.setBackgroundColor(Color.parseColor("#f4f5f8"))
            } else {
                Log.i(
                    TAG,
                    "loadDocsFile fileID:${fileID} failed，errorCode：${errorCode}"
                )
            }
            listener.onLoadFile(errorCode)
        })
    }

    /**
     * 将文件从视图中卸载
     */
    fun unloadFile() {
        zegoDocsView?.unloadFile()
    }

    fun resizeLayout(size: Size) {
        val params = layoutParams
        params.width = size.width
        params.height = size.height
        layoutParams = params

//        reload(size)
    }

    private fun reload(size: Size) {
        if (zegoDocsView != null) {
            zegoDocsView?.let {
                it.reloadFile { loadCode ->
                    if (loadCode == 0) {
                        Log.d(TAG, "visibleRegion:${it.visibleSize}")
                        currentWhiteboardSize = it.visibleSize
                        currentWhiteboardView?.setVisibleRegion(it.visibleSize)
                    }
                }
            }
        } else {
            val model = getCurrentWhiteboardModel()
            val aspectWidth = model.aspectWidth / model.pageCount.toFloat()
            val aspectHeight = model.aspectHeight
            //宽高比
            val aspectRatio = aspectWidth / aspectHeight
            val showSize = calcShowSize(size, aspectRatio)
            Log.d(TAG, "reload pure whiteboard: aspectRatio=$aspectRatio, showSize=$showSize")
            currentWhiteboardSize = showSize
            currentWhiteboardView?.setVisibleRegion(showSize)
        }
    }

    private fun calcShowSize(parentSize: Size, aspectRatio: Float): Size {
        return if (aspectRatio > parentSize.width.toFloat() / parentSize.height) {
            // 填充宽
            Size(parentSize.width, (parentSize.width.toFloat() / aspectRatio).toInt())
        } else {
            // 填充高
            Size(ceil(aspectRatio * parentSize.height).toInt(), parentSize.height)
        }
    }

    fun setDocsViewAuthInfo(authInfo: HashMap<String, Int>){
        zegoDocsView?.setOperationAuth(authInfo)
    }
}