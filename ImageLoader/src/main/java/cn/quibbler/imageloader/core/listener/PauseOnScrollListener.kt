package cn.quibbler.imageloader.core.listener

import android.widget.AbsListView
import cn.quibbler.imageloader.core.ImageLoader

/**
 * Listener-helper for {@linkplain AbsListView list views} ({@link ListView}, {@link GridView}) which can
 * {@linkplain ImageLoader#pause() pause ImageLoader's tasks} while list view is scrolling (touch scrolling and/or
 * fling). It prevents redundant loadings.<br />
 * Set it to your list view's {@link AbsListView#setOnScrollListener(OnScrollListener) setOnScrollListener(...)}.<br />
 * This listener can wrap your custom {@linkplain OnScrollListener listener}.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.7.0
 */
class PauseOnScrollListener(
    private val imageLoader: ImageLoader,
    private val pauseOnScroll: Boolean, private val pauseOnFling: Boolean,
    private var externalListener: AbsListView.OnScrollListener?
) : AbsListView.OnScrollListener {

    /**
     * Constructor
     *
     * @param imageLoader    {@linkplain ImageLoader} instance for controlling
     * @param pauseOnScroll  Whether {@linkplain ImageLoader#pause() pause ImageLoader} during touch scrolling
     * @param pauseOnFling   Whether {@linkplain ImageLoader#pause() pause ImageLoader} during fling
     * @param customListener Your custom {@link OnScrollListener} for {@linkplain AbsListView list view} which also
     *                       will be get scroll events
     */
    constructor(imageLoader: ImageLoader, pauseOnScroll: Boolean, pauseOnFling: Boolean) : this(imageLoader, pauseOnScroll, pauseOnFling, null)

    override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
        when (scrollState) {
            AbsListView.OnScrollListener.SCROLL_STATE_IDLE -> {
                imageLoader.resume()
            }
            AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL -> {
                if (pauseOnScroll) {
                    imageLoader.pause()
                }
            }
            AbsListView.OnScrollListener.SCROLL_STATE_FLING -> {
                if (pauseOnFling) {
                    imageLoader.pause()
                }
            }
        }
        externalListener?.let {
            it.onScrollStateChanged(view, scrollState)
        }
    }

    override fun onScroll(view: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
        externalListener?.let {
            it.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount)
        }
    }

}