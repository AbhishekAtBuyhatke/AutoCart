package com.buyhatke.autocart.Models


import com.google.gson.annotations.SerializedName

data class Variants(@SerializedName("gid")
                    val gid: String = "",
                    @SerializedName("color")
                    val color: String = "",
                    @SerializedName("space")
                    val space: String = ""


) {
    override fun toString(): String {
        return "Variants(gid='$gid', color='$color', space='$space')"
    }
}


