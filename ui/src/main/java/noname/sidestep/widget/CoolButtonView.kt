package noname.sidestep.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin


// overdraws everything and everywhere, but should look plenty cool
class CoolButtonView : View {

    data class Vec2(val x: Float, val y: Float) {
        fun minus(v2: Vec2) : Vec2 = Vec2(x - v2.x, y - v2.y)
        fun plus(v2: Vec2) : Vec2 = Vec2(x + v2.x, y + v2.y)
        fun mult(scalar: Float) : Vec2 = Vec2(x * scalar, y * scalar)
        fun div(scalar: Float) : Vec2 = Vec2(x / scalar, y / scalar)
        fun rot(ang: Float) : Vec2 = Vec2( (x * cos(ang) - y * sin(ang)), x * sin(ang) + y * cos(ang))
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    data class State(
        val position: Vec2

    )

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
    }


}