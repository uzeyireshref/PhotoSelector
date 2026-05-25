package android.net

import android.os.Parcel

class FakeUri(private val value: String) : Uri() {
    override fun buildUpon(): Builder = throw UnsupportedOperationException("Not needed in tests")
    override fun getAuthority(): String? = null
    override fun getEncodedAuthority(): String? = null
    override fun getEncodedFragment(): String? = null
    override fun getEncodedPath(): String = value
    override fun getEncodedQuery(): String? = null
    override fun getEncodedSchemeSpecificPart(): String = value
    override fun getEncodedUserInfo(): String? = null
    override fun getFragment(): String? = null
    override fun getHost(): String? = null
    override fun getLastPathSegment(): String = value.substringAfterLast('/')
    override fun getPath(): String = value
    override fun getPathSegments(): List<String> = value.split('/').filter { it.isNotBlank() }
    override fun getPort(): Int = -1
    override fun getQuery(): String? = null
    override fun getScheme(): String = "content"
    override fun getSchemeSpecificPart(): String = value
    override fun getUserInfo(): String? = null
    override fun isHierarchical(): Boolean = true
    override fun isRelative(): Boolean = false
    override fun describeContents(): Int = 0
    override fun writeToParcel(parcel: Parcel, flags: Int) = Unit
    override fun toString(): String = "content://test/$value"
    override fun equals(other: Any?): Boolean = other is FakeUri && other.value == value
    override fun hashCode(): Int = value.hashCode()
}
