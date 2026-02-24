package com.piratemaker.postermaker.listener.listenerdraw

import com.ocmaker.pixcel.maker.data.model.draw.Draw
import com.ocmaker.pixcel.maker.data.model.draw.DrawableDraw


interface OnDrawListener {
    fun onAddedDraw(draw: Draw)

    fun onClickedDraw(draw: Draw)

    fun onDeletedDraw(draw: Draw)

    fun onDragFinishedDraw(draw: Draw)

    fun onTouchedDownDraw(draw: Draw)

    fun onZoomFinishedDraw(draw: Draw)

    fun onFlippedDraw(draw: Draw)

    fun onDoubleTappedDraw(draw: Draw)

    fun onHideOptionIconDraw()

    fun onUndoDeleteDraw(draw: List<Draw?>)

    fun onUndoUpdateDraw(draw: List<Draw?>)

    fun onUndoDeleteAll()

    fun onRedoAll()

    fun onReplaceDraw(draw: Draw)

    fun onEditText(draw: DrawableDraw)

    fun onReplace(draw: Draw)
}