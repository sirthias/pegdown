## Times out with QUOTE extension enabled.

Without smart quotes it flies though it.

| Değişken | Açıklama |
| -- | -- |
| $! | `raise` ile atanan **exception** bilgisidir. `rescue`ile erişilir. |
| $@ | Son **exception**'a ait **backtrace** (_bir tür log_) bilgilerinin tutulduğu dizi (_Array_) |
| $& | Son yakalanan **match**'in tutulduğu **String** (_Regex konusunda göreceğiz_) |
| $\` | Son yakalanan **match**'in solunda kalan kısım |
| $' | Son yakalanan **match**'in sağında kalan kısım |
| $+ | En yüksek **group match**'in tutulduğu yer. (_Regex yaparken group yakalama konusunda göreceğiz_) |
| $1, $2, ..., $9 | Yine, Regex ile patern yakalama (_pattern matching_) yaptığımızda, yakaladığımız şeylerin sıra numarası. |
| $~ | O anki kapsama alanında (_scope_) son yakalananla ilgili bilgilerin tutulduğu değişken |
| $= | Regex ile uğraşırken, karakterlerin büyük/küçük harfe duyarlılığı ile ilgi ayarlar vardır. Örneğin büyük/küçük harf farkı olmadan aramak yaparken (_case insensitive_) bu değişkene atama yaparız. Varsayılan değer (_default_) `nil`'dir |
| $/ | Dosyadan okuma yapılırken satırların nasıl ayrıldığının tespit edildiği değişkendir. Eğer `nil` olarak atarnırsa, dosya okuması esnasında satır-satır okuma yerine tüm dosya biranda okunur. |
| $\ | Bu da çıktı için ayraçtır. `print`ve `puts` gibi komutlarda `IO#write` gibi işlemlerde kullanılır. Varsayılan değer `nil`'dir |
| $, | `print` ve `Array#join` de kullanılan ayraçtır. |
| $; | `String#split` de kullanılan ayraçtır. |
| $. | Dosya işlemlerinde son okunan dosyanın aktif satır numarısını verir. |
| $< | Aynı `shell` deki ekleme (_concatenation_) işlemi gibi sanal ekleme yapar. |
| $> | `print` ve `printf` işlemi için varsayılan çıktıdır. Varsayılan değeride `$stdout` |
| $_ | `gets` veya `readline` ile alınan son satırdır, cinsi **String**'dir. |
| $0 | Çalıştırılan script'in dosya adıdır. |
| $* | Komut satırı işlemlerinde, dosyaya geçilen argümanların saklandığı değişkendir. |
| $$ | Çalıştırılan script'in işlem numarası (_Process Number_) |
| $? | Çalıştırılan son alt işlemin (_Child Process_) durumu. |
| $: | Modüller ve ek dosyalar için **Path** (_Load Path__) bilgisi. `require` komutunda göreceğiz. |
| $" | `require` ile yüklenen dosyaların adlarının tutulduğu dizi (_Array_) |
| $DEBUG | Adında da anlaşıldığı gibi, eğer **DEBUG** modda çalıştırma yapıyorsak (_ki bunu -d ile yaparız_) oluşan her **exception**'ın `$stderr` değişkenine atanmasını sağlar. |
| $KCODE | Kod yazdığımız script dosyasının **encoding** tipini seçmemize yarar. Son sürümlerde ihtiyaç kalmadı, herşey **default** olarak `UTF-8` çalışıyor. |
| $FILENAME | Komut satırından argüman olarak dosya geçtiğimizde geçilen dosyanın adını almak için kullanılır. Aslında `ARGF.filename` ile aynı işi yapar. |
| $LOAD_PATH | `$:` ile aynı işi yapan **alias**'dır (_alias = takma ad_) |
| $SAFE | Güvenlik seviyesidir. Varsayılan değer `0` dır. Bu dereceler 0'dan 4'e kadardır. Kod güvenliği ve kilitleme yapmak için kullanılır. Biraz karmaşık bir konudur :) Örneğin, emin olmadığınız bir kütüphane kullanırken kodunuzu güvenli hale getirmek için, kod bloğunun önüne `$SAFE=4` ekerseniz, takip eden kod `array` `hash` ve `string` lerde hiç bir modifikasyon yapamaz! Hatta pek [çok şeyi](http://phrogz.net/programmingruby/taint.html) yapamaz! |
| $stdin | Standart giriş. |
| $stdout | Standart çıkış. |
| $stderr | Giriş/Çıkış hata bildirimi. |
| $VERBOSE | Kernel tarafından üretilen tüm uyarı mesajlarının (_warning gibi..._) görüntülenmesi için kullanılır. |
| $-0 | `$/` ile aynı işi yapar. |
| $-a | Komut satırından çalıştırma yaparkan `-a` ataması yapılmışsa `$-a` **true** döner. |
| $-d | `$DEBUG` ile aynı işi yapar. |
| $-F | `$;` ile aynı işi yapar. |
| $-i | Bu değişken **in-place-edit** modda **extension**'ı saklar. |
| $-I | `$:` ile aynı işi yapar. (_Büyük i_) |
| $-l | Eğer `-lis` set edilmişse `true` döner. **Read Only** yani sadece okunur, değeri değiştirilemez! (_Küçük l_) |
| $-p | Eğer `-pis` set edilmişse `true` döner. **Read Only** yani sadece okunur, değeri değiştirilemez! |
| $-K | `$KCODE` ile aynı işi yapar. |
| $-v | `$VERBOSE` ile aynı işi yapar. |
| $-w | Eğer `-w` set edilmişse `true` döner. |
| $-W | **Warning Level** yani oluşabilecek hatalar vs ile ilgili 0, 1 ya da 2.seviyede uyarı mesajları göstermek için. |
| $LOADED_FEATURES | `$"` ile aynı işi yapar. |
| $PROGRAM_NAME | `$0` ile aynı işi yapar. |
