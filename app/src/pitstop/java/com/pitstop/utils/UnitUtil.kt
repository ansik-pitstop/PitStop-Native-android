package com.pitstop.utils

enum class UnitOfLength {
    Miles {
        override fun toString(): String {
            return "miles"
        }
    },
    Kilometers {
        override fun toString(): String {
            return "km"
        }
    };

    companion object {
        @JvmStatic
        fun getValueFromToString(toString: String): UnitOfLength? {
            if (toString == UnitOfLength.Kilometers.toString()) {
                return Kilometers
            }
            if (toString == UnitOfLength.Miles.toString()) {
                return Miles
            }
            return null
        }

        @JvmStatic
        fun convertKilometreToMiles(kilometreString: String): Double {
            try {
                val kilometre = kilometreString.toDouble()
                return kilometre/1.609
            } catch (e: Error) {}
            return 0.0
        }
    }


    fun uiString(): String {
        return when(this) {
            Miles -> "Miles"
            Kilometers -> "Kilometres"
        }
    }
}