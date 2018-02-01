package io.github.landerlyoung.droidstreamer.service

import android.os.Parcel
import android.os.Parcelable

/**
 * ```
 * Author: landerlyoung@gmail.com
 * Date:   2017-06-24
 * Time:   15:04
 * Life with Passion, Code with Creativity.
 * ```
 */
data class StreamState(val isStream: Boolean) : Parcelable {
    companion object {
        @JvmField val CREATOR: Parcelable.Creator<StreamState> = object : Parcelable.Creator<StreamState> {
            override fun createFromParcel(source: Parcel): StreamState = StreamState(source)
            override fun newArray(size: Int): Array<StreamState?> = arrayOfNulls(size)
        }
    }

    constructor(source: Parcel) : this(
            1 == source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt((if (isStream) 1 else 0))
    }
}
