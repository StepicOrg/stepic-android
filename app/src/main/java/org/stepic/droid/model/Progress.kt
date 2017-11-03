package org.stepic.droid.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class Progress(
        val id: String? = null,
        @SerializedName("last_viewed")
        val lastViewed: String? = null,
        var score: String? = null,
        val cost: Int = 0,
        @SerializedName("n_steps")
        val nSteps: Int = 0,
        @SerializedName("n_steps_passed")
        val nStepsPassed: Int = 0,
        @SerializedName("is_passed")
        val isPassed: Boolean = false
) : Parcelable {

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(lastViewed)
        parcel.writeString(score)
        parcel.writeInt(cost)
        parcel.writeInt(nSteps)
        parcel.writeInt(nStepsPassed)
        parcel.writeByte(if (isPassed) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Progress> {
        override fun createFromParcel(parcel: Parcel): Progress = Progress(
                parcel.readString(),
                parcel.readString(),
                parcel.readString(),
                parcel.readInt(),
                parcel.readInt(),
                parcel.readInt(),
                parcel.readByte() != 0.toByte()
        )

        override fun newArray(size: Int): Array<Progress?> = arrayOfNulls(size)
    }

}
