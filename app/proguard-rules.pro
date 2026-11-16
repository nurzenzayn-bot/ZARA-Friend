# Aturan R8/ProGuard untuk Zara Friend.
# Fase 7 melakukan review penuh; untuk sekarang cukup aturan dasar Hilt.
# Hilt bekerja dengan R8 tanpa aturan tambahan (consumer rules bawaan SDK).

# Jaga pesan error coroutine (opsional, mempermudah debugging)
-dontwarn org.jetbrains.annotations.*
