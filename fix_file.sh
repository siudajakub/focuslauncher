#!/bin/bash
sed -i 's/if (resource == R.string.file_type_none && label.matches(Regex(".+\\..+")))/if (resource == R.string.file_type_none \&\& label.matches(FILE_EXTENSION_REGEX))/' core/base/src/main/java/de/mm20/launcher2/search/File.kt
awk '
/^import java.util.Locale/ {
    print $0
    print ""
    print "private val FILE_EXTENSION_REGEX = Regex(\".+\\\\..+\")"
    next
}
{print}
' core/base/src/main/java/de/mm20/launcher2/search/File.kt > core/base/src/main/java/de/mm20/launcher2/search/File.kt.tmp
mv core/base/src/main/java/de/mm20/launcher2/search/File.kt.tmp core/base/src/main/java/de/mm20/launcher2/search/File.kt
