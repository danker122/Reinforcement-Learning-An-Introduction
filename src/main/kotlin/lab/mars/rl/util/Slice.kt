@file:Suppress("NOTHING_TO_INLINE", "OVERRIDE_BY_INLINE")

package lab.mars.rl.util

import java.util.*

/**
 * <p>
 * Created on 2017-09-11.
 * </p>
 *
 * @author wumo
 */
fun IntArray.slice(start: Int, end: Int): DefaultIntSlice = DefaultIntSlice.reuse(this, start, end)

interface IntSlice : Index {
    operator fun get(start: Int, end: Int): IntSlice

    fun toIntArray(): IntArray
    fun copy(): IntSlice
}

interface MutableIntSlice : IntSlice {
    val cap: Int

    operator fun set(idx: Int, s: Int)

    operator fun set(start: Int, end: Int, s: Int)

    fun remove(range: IntRange) {
        remove(range.start, range.endInclusive)
    }

    fun remove(start: Int, end: Int)
    fun remove(index: Int) = remove(index, index)
    fun removeLast(num: Int) = remove(lastIndex - num + 1, lastIndex)

    fun add(s: Int)

    fun add(num: Int, s: Int)

    fun reuseBacked(): IntSlice
}

interface AppendableIntSlice : MutableIntSlice {
    fun ensure(minCap: Int)

    fun append(s: Int)

    fun append(num: Int, s: Int)
}

open class DefaultIntSlice constructor(internal var backed: IntArray, internal var offset: Int, size: Int, cap: Int = size) :
        AppendableIntSlice {

    companion object {
        val MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8
        /**
         * @param s 用枚举的参数构成初始的[DefaultIntSlice]
         */
        fun of(vararg s: Int) = DefaultIntSlice(s, 0, s.size)

        /**
         * @param num 初始化[num]长度、初值为0的[DefaultIntSlice]
         */
        inline fun zero(num: Int) = new(num, num)

        inline fun reuse(array: IntArray, start: Int, end: Int) = DefaultIntSlice(array, start, end - start + 1)

        inline fun reuse(array: IntArray) = reuse(array, 0, array.lastIndex)

        fun new(cap: Int = 2, size: Int = 0) = DefaultIntSlice(IntArray(cap), 0, size, cap)
        fun from(s: Index) = reuse(IntArray(s.size) { s[it] })
    }

    init {
        require(
                offset in 0..backed.lastIndex &&
                size in 0..backed.size - offset &&
                cap in size..backed.size - offset)
    }

    private var _size = size
    private var _cap = cap

    override val size: Int
        get() = _size
    override val cap: Int
        get() = _cap

    override operator fun get(dim: Int): Int {
        require(dim in 0 until _size)
        return backed[dim + offset]
    }

    override operator fun get(start: Int, end: Int): DefaultIntSlice {
        require(start in 0..end)
        require(end < _size)
        return DefaultIntSlice(backed, offset + start, _size - (end - start))
    }

    override operator fun set(idx: Int, s: Int) {
        require(idx in 0 until _size)
        backed[idx + offset] = s
    }

    override operator fun set(start: Int, end: Int, s: Int) {
        require(start in 0..end)
        require(end < _size)
        for (idx in start..end)
            backed[offset + idx] = s
    }

    override fun remove(start: Int, end: Int) {
        require(start in 0..end)
        require(end < _size)
        if (end < _size - 1) {
            val moved = _size - 1 - end
            System.arraycopy(backed, offset + end + 1, backed, offset + start, moved)
        }
        _size -= (end - start + 1)
    }

    override fun toIntArray(): IntArray = Arrays.copyOfRange(backed, offset, offset + _size)

    override fun copy() = reuse(toIntArray())

    override fun reuseBacked() = DefaultIntSlice(backed, offset, size, cap)

    override fun add(s: Int) {
        require(_size < _cap)
        backed[offset + _size] = s
        _size++
    }

    override fun add(num: Int, s: Int) {
        require(_size + num <= _cap)
        for (i in 0 until num)
            backed[offset + _size + i] = s
        _size += num
    }

    override fun ensure(minCap: Int) {
        if (_cap >= minCap) return
        _cap += _cap
        if (minCap > _cap)
            _cap = minCap
        if (_cap > MAX_ARRAY_SIZE)
            _cap = Int.MAX_VALUE
        if (offset + _cap > backed.size) {
            val tmp = IntArray(_cap)
            System.arraycopy(backed, offset, tmp, 0, _size)
            backed = tmp
            offset = 0
        }
    }

    final inline override fun append(s: Int) {
        ensure(size + 1)
        add(s)
    }

    final inline override fun append(num: Int, s: Int) {
        ensure(size + num)
        add(num, s)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("[")
        for (idx in offset until lastIndex)
            sb.append(backed[idx]).append(", ")
        sb.append(backed[lastIndex])
        sb.append("]")
        return sb.toString()
    }


}