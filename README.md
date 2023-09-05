# ШПАРГАЛКИ

### Замечания

1) ОБЯЗАТЕЛЬНО! Если в полях `data class` присутствует массив (`(Byte|Char|Int|...)Array`) обязательно надо переопределить и `equals`, и `hashCode`
```kotlin
override fun hashCode() : Int {
    var hash = some_field.hashCode()
    hash = 31 * hash + array.contentHashCode() //(!!)
    return hash
}

override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as SomeDataClass
    return some_field == other.some_field
            && array contentEquals other.array //(!!)
}
```
Иначе коллекцию из объектов этих классов тяжело будет сравнивать. Причина в этих массивах - без переопределения сравниваться будут ссылки массивов, а не их содержимое.