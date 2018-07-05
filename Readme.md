Android Slide Action
=========================

## Android Slide Action Recycler View

[![Release](https://jitpack.io/v/softwarejoint/android-rv-swipe-delete.svg)](https://jitpack.io/#softwarejoint/android-rv-swipe-delete)

This library provides a simple way to add a draggable sliding up panel (popularized by Google Music and Google Maps) to your Android application.

Based On https://github.com/kitek/android-rv-swipe-delete

<p align="center">
  <img src="/screenshot/device-2018-06-30-075510.png" width="320" title="Example">
</p>

Lots of work has been put in making it seem smooth and close to iOS in feel.

### Notes

* Supports custom background color & icon 
* Supports swipe to delete & click to delete
* Only 1 row active at a time
* It is written using canvas, no extra views drawn. Uses pure Java code

### Using in your project

1. Include jitpack.io maven repo

```
    repositories {
        maven { url "https://jitpack.io" }
    }

```

2. Add dependency to project

```
    dependencies {
	    compile 'com.github.softwarejoint:/android-rv-swipe-delete:1.1.1'
	}

```

3. Bind to RecyclerView

```
        Drawable deleteIcon = ContextCompat.getDrawable(this, R.drawable.ic_delete_white_24);
        SwipeTouchHelper swipeTouchHelper = new SwipeTouchHelper(recyclerView, deleteIcon, this);
```

4. Add CallBackListener

```
    @Override
    public void onSwipeActionClicked(final RecyclerView.ViewHolder viewHolder) {
        Log.d(TAG, "onSwipeActionClicked: " + viewHolder.getItemId());
        
        final int position = viewHolder.getAdapterPosition();
        final long itemId = viewHolder.getItemId();

        //Network Op...
        if (doNetworkOp()) {
            //actionCompleted
            adapter.removeAt(position);
            swipeTouchHelper.markActionComplete(itemId);
        } else {
            //actionCancelled
            swipeTouchHelper.undoAction(viewHolder)
        }
    }
    
```

5. Set Custom Color


```
    swipeTouchHelper.setSwipeBackGroundColor(@ColorInt int resourceId) 
```

6. Undo Action

```
    swipeTouchHelper.undoAction(viewHolder);
```