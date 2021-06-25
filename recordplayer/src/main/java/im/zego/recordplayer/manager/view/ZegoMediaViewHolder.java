package im.zego.recordplayer.manager.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zego.recorderplayback.ZegoPlaybackStreamInfo;

import java.util.Objects;

import im.zego.commonlibs.sdk.whiteboard.WhiteboardSDKManager;
import im.zego.commonlibs.utils.Logger;
import im.zego.commonlibs.utils.ToastUtils;
import im.zego.commonlibs.whiteboard.WhiteboardContainer;
import im.zego.recordplayer.manager.IMediaViewListener;
import im.zego.recordplayer.manager.VideoManager;
import im.zego.recordplayer.manager.utils.CommonUtil;
import im.zego.zegowhiteboard.ZegoWhiteboardView;
import im.zego.zegowhiteboard.callback.IZegoWhiteboardManagerListener;

/**
 * 所有回放View的父View
 */
public class ZegoMediaViewHolder extends ZOrderLayout implements IMediaViewListener {

    private static final String TAG = "ZegoMediaViewHolder";

    public ZegoMediaViewHolder(@NonNull Context context) {
        this(context, null);
    }

    public ZegoMediaViewHolder(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZegoMediaViewHolder(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private Context mContext;

    // 回放的白板View
    private WhiteboardContainer mWhiteboardContainer;

    // 父View等比放大或缩小的比例
    private float mScaleRatio;

    protected void init(Context context) {
        VideoManager.getInstance().setMediaViewListener(this);

        if (getActivityContext() != null) {
            this.mContext = getActivityContext();
        } else {
            this.mContext = context;
        }

        mWhiteboardContainer = new WhiteboardContainer(mContext);
        mWhiteboardContainer.setVisibility(INVISIBLE);
        addView(mWhiteboardContainer, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 0));

        addWhiteboardViewListener();
    }

    private void addWhiteboardViewListener() {
        WhiteboardSDKManager.getInstance().setWhiteboardCountListener(new IZegoWhiteboardManagerListener() {

            @Override
            public void onWhiteboardAdded(ZegoWhiteboardView zegoWhiteboardView) {
                Log.i(TAG, "onWhiteboardAdded: zegoWhiteboardView=" + zegoWhiteboardView.getWhiteboardViewModel());
                mWhiteboardContainer.onReceiveWhiteboardView(zegoWhiteboardView, (errorCode, newHolder, holder) -> null);
            }

            @Override
            public void onWhiteboardRemoved(long whiteboardID) {
                Log.i(TAG, "onWhiteboardRemoved: whiteboardID=" + whiteboardID);
                mWhiteboardContainer.removeWhiteboardViewHolder(whiteboardID);
            }

            @Override
            public void onError(int errorCode) {
                ToastUtils.showCenterToast("errorCode: " + errorCode);
            }
        });
    }

    protected Context getActivityContext() {
        return CommonUtil.getActivityContext(getContext());
    }

    /**
     * 从 ZegoMediaInfo 中得到的View总宽高是一个相对的值
     * 在这里，需要依赖于当前屏幕的尺寸，动态计算 mParentContainer 实际撑满的宽高 (等比放大或缩小)
     * <p>
     * 具体的计算规则见 {@link #calcShowSize(Size, float)}
     */
    @Override
    public void onLoaded() {
        int parentWidth = VideoManager.getInstance().getMaxWidth();
        int parentHeight = VideoManager.getInstance().getMaxHeight();
        // 父View实际的宽高
        Size parentSize = calcShowSize(new Size(getWidth(), getHeight()), (float) parentWidth / parentHeight);
        // 等比放大或缩小的比例，因为是等比的，只需要计算一边即可
        mScaleRatio = (float) parentSize.getWidth() / parentWidth;
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.width = parentSize.getWidth();
        layoutParams.height = parentSize.getHeight();
        setLayoutParams(layoutParams);
    }

    @Override
    public void onWhiteboardSwitch(long whiteboardId) {
        mWhiteboardContainer.selectWhiteboardViewHolder(whiteboardId);
    }

    @Override
    public void onVideoViewCreate(ZegoPlaybackStreamInfo info) {
        if (info.mediaInfo != null) {
            TextureView textureView;
            View view = findView(this, getId(info));
            if (view == null) {
                textureView = new TextureView(mContext);
                textureView.setTag(info.mediaInfo.streamId);
                addView(textureView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, info.zorder));
            } else {
                textureView = (TextureView) view;
            }
            textureView.setVisibility(GONE);
            changeMediaViewLayout(info, textureView);

            info.mediaInfo.play.setMediaView(textureView);
        }
    }

    @Override
    public void onVideoViewDelete(ZegoPlaybackStreamInfo info) {
        View view = findView(this, getId(info));
        if (view != null) {
            view.setVisibility(GONE);
        }
//        removeView(view);
    }

    @Override
    public void onLayoutChange(ZegoPlaybackStreamInfo[] infoList) {
        for (ZegoPlaybackStreamInfo info : infoList) {
            View view = findView(this, info);
            if (view != null) {
                changeMediaViewLayout(info, view);
            }
        }
    }

    private String getId(ZegoPlaybackStreamInfo info) {
        if (info.whiteboardInfo != null) {
            return String.valueOf(info.whiteboardInfo.whiteboardId);
        } else if (info.mediaInfo != null) {
            return info.mediaInfo.streamId;
        }
        return null;
    }

    private void changeMediaViewLayout(ZegoPlaybackStreamInfo info, View view) {
        if (view != null) {
            ViewLayoutInfo layoutInfo = calculateLayoutInfo(info);
            Logger.i(TAG, String.format("changeMediaViewLayout, info = %s, layoutInfo = %s", CommonUtil.printInfo(info), layoutInfo));

            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();

            layoutParams.zOrder = info.zorder;
            layoutParams.leftMargin = layoutInfo.left;
            layoutParams.topMargin = layoutInfo.top;
            layoutParams.height = layoutInfo.height;
            layoutParams.width = layoutInfo.width;

            updateZOrder();

            view.setLayoutParams(layoutParams);

            view.setVisibility(info.display ? VISIBLE : INVISIBLE);
        }
    }

    /**
     * info返回的信息是相对的，需要计算出该View实际的left top right bottom
     */
    private ViewLayoutInfo calculateLayoutInfo(ZegoPlaybackStreamInfo info) {
        ViewLayoutInfo layoutInfo = new ViewLayoutInfo();
        layoutInfo.width = (int) (info.width * mScaleRatio);
        layoutInfo.height = (int) (info.height * mScaleRatio);
        layoutInfo.left = (int) (info.x * mScaleRatio);
        layoutInfo.top = (int) (info.y * mScaleRatio);
        return layoutInfo;
    }

    private Size calcShowSize(Size parentSize, float aspectRatio) {
        if (aspectRatio > (float) parentSize.getWidth() / parentSize.getHeight()) {
            // 填充宽
            return new Size(parentSize.getWidth(), (int) ((float) parentSize.getWidth() / aspectRatio));
        } else {
            // 填充高
            return new Size((int) Math.ceil(aspectRatio * parentSize.getHeight()), parentSize.getHeight());
        }
    }

    private View findView(ViewGroup viewGroup, String infoId) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View view = viewGroup.getChildAt(i);
            if (Objects.equals(infoId, view.getTag())) {
                return view;
            }
        }
        return null;
    }

    private View findView(ViewGroup viewGroup, ZegoPlaybackStreamInfo info) {
        if (info.whiteboardInfo != null) {
            return mWhiteboardContainer;
        } else {
            return findView(viewGroup, getId(info));
        }
    }

    static class ViewLayoutInfo {
        int left;
        int top;
        int width;
        int height;

        @Override
        public String toString() {
            return "ViewLayoutInfo{" +
                    "left=" + left +
                    ", top=" + top +
                    ", width=" + width +
                    ", height=" + height +
                    '}';
        }
    }
}
