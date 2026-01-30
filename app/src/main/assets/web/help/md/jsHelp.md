# Biến và hàm js
> Legado sử dụng [Rhino v1.8.0](https://github.com/mozilla/rhino) làm công cụ JavaScript để thuận tiện cho việc [gọi các lớp và phương thức Java](https://m.jb51.net/article/92138.htm), xem [bảng tương thích ECMAScript](https://mozilla.github.io/rhino/compat/engines.html)

> [Rhino Runtime](https://github.com/mozilla/rhino/blob/master/rhino/src/main/java/org/mozilla/javascript/ScriptRuntime.java) tải chậm các lớp và phương thức Java được nhập

|Hàm khởi tạo|Hàm|Đối tượng|Lớp gọi|Mô tả ngắn gọn|
|------|-----|------|----|------|
|JavaImporter|importClass importPackage| |[ImporterTopLevel](https://github.com/mozilla/rhino/blob/master/rhino/src/main/java/org/mozilla/javascript/ImporterTopLevel.java)|Nhập lớp Java vào JavaScript|
||getClass|Packages java javax ...|[NativeJavaTopPackage](https://github.com/mozilla/rhino/blob/master/rhino/src/main/java/org/mozilla/javascript/NativeJavaTopPackage.java)|Mặc định nhập các lớp Java trong JavaScript|
|JavaAdapter|||[JavaAdapter](https://github.com/mozilla/rhino/blob/master/rhino/src/main/java//org/mozilla/javascript/JavaAdapter.java)|Kế thừa lớp Java|

> Lưu ý biến `java` trỏ đến đã bị Legado sửa đổi, nếu muốn gọi các gói dưới `java.*`, vui lòng sử dụng `Packages.java.*`

> Trong quy tắc nguồn sách sử dụng `@js` `<js>` `{{}}` có thể sử dụng JavaScript để gọi các lớp và phương thức tích hợp sẵn của Legado

> Lưu ý để đảm bảo an toàn, Legado sẽ chặn một số lời gọi lớp java, xem [RhinoClassShutter](https://github.com/gedoor/legado/blob/master/modules/rhino/src/main/java/com/script/rhino/RhinoClassShutter.kt)

> Các lớp và phương thức Java được hỗ trợ gọi có thể khác nhau trong các quy tắc nguồn sách khác nhau

> Lưu ý biến được khai báo bằng `const` không hỗ trợ phạm vi khối, sử dụng trong vòng lặp sẽ gặp vấn đề giá trị không đổi, vui lòng chuyển sang dùng `var` để khai báo

|Tên biến|Lớp gọi|
|------|-----|
|java|Lớp hiện tại|
|baseUrl|url hiện tại, String|
|result|Kết quả bước trước|
|book|[Lớp sách](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/Book.kt)|
|rssArticle|[Lớp Article](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/RssArticle.kt)|
|chapter|[Lớp chương](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/BookChapter.kt)|
|source|[Lớp nguồn sách cơ sở](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/BaseSource.kt)|
|cookie|[Lớp thao tác cookie](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/help/http/CookieStore.kt)| 
|cache|[Lớp thao tác bộ nhớ đệm](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/help/CacheManager.kt)|
|title|Tiêu đề chương hiện tại String|
|src| Mã nguồn trả về từ yêu cầu|
|nextChapterUrl|url chương tiếp theo|

## Một số phương thức khả dụng của đối tượng lớp hiện tại

### [RssJsExtensions](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/ui/rss/read/RssJsExtensions.kt)
> Chỉ có thể sử dụng trong quy tắc `shouldOverrideUrlLoading` của nguồn đăng ký  
> Thêm chặn url chuyển hướng đăng ký, js, trả về true để chặn, biến js url, có thể mở url thông qua js  
> Quy tắc chặn chuyển hướng url không được thực hiện các thao tác tốn thời gian
> Ví dụ https://github.com/gedoor/legado/discussions/3259

* Gọi tìm kiếm Legado

```js
java.searchBook(bookName: String)
```

* Thêm vào kệ sách

```js
java.addBook(bookUrl: String)
```

### Một số hàm [AnalyzeUrl](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt)
> Trong js gọi qua java., chỉ có hiệu lực trong quy tắc `JS kiểm tra đăng nhập`
```js
initUrl() //Phân tích lại url, có thể dùng để phân tích lại url truy cập sau khi đăng nhập bằng js kiểm tra đăng nhập
getHeaderMap().putAll(source.getHeaderMap(true)) //Đặt lại header đăng nhập
getStrResponse( jsStr: String? = null, sourceRegex: String? = null) //Trả về kết quả truy cập, loại văn bản, sau khi đăng nhập lại bên trong nguồn sách có thể gọi phương thức này để trả về kết quả
getResponse(): Response //Trả về kết quả truy cập, công cụ đọc mạng sử dụng cái này, sau khi gọi đăng nhập có thể gọi phương thức này để truy cập lại, tham khảo kiểm tra đăng nhập Aliyun
```

### Một số hàm [AnalyzeRule](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeRule.kt)
* Lấy văn bản/danh sách văn bản
> `mContent` Mã nguồn chờ phân tích, mặc định là trang hiện tại  
> `isUrl` Cờ liên kết, mặc định là `false`
```js
java.getString(ruleStr: String?, mContent: Any? = null, isUrl: Boolean = false)
java.getStringList(ruleStr: String?, mContent: Any? = null, isUrl: Boolean = false)
```
* Đặt nội dung phân tích

```js
java.setContent(content: Any?, baseUrl: String? = null):
```

* Lấy Element/Danh sách Element

> Nếu muốn thay đổi mã nguồn phân tích, vui lòng sử dụng `java.setContent` trước

```js
java.getElement(ruleStr: String)
java.getElements(ruleStr: String)
```

* Tìm kiếm lại sách/Lấy lại url mục lục

> Chỉ có thể sử dụng trước khi làm mới mục lục, địa chỉ một số nguồn sách và url mục lục sẽ thay đổi

```js
java.reGetBook()
java.refreshTocUrl()
```
* Truy cập biến

```js
java.get(key)
java.put(key, value)
```

### Một số hàm [Lớp mở rộng js](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/help/JsExtensions.kt)

* Phân tích liên kết [JsURL](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/utils/JsURL.kt)
```js
java.toURL(url): JsURL
java.toURL(url, baseUrl): JsURL
```
* Lấy SystemWebView User-Agent
```js
java.getWebViewUA(): String
```
* Yêu cầu mạng
```js
java.ajax(urlStr): String
java.ajaxAll(urlList: Array<String>): Array<StrResponse>
//Trả về StrResponse phương thức body() code() message() headers() raw() toString() 
java.connect(urlStr): StrResponse

java.post(url: String, body: String, headerMap: Map<String, String>): Connection.Response

java.get(url: String, headerMap: Map<String, String>): Connection.Response

java.head(url: String, headerMap: Map<String, String>): Connection.Response

* Sử dụng webView truy cập mạng
* @param html html được tải trực tiếp bằng webView, nếu html trống truy cập trực tiếp url
* @param url nếu trong html có tài nguyên đường dẫn tương đối không truyền url sẽ không truy cập được
* @param js câu lệnh js dùng để lấy giá trị trả về, không có sẽ trả về toàn bộ mã nguồn
* @return Trả về nội dung js lấy được
java.webView(html: String?, url: String?, js: String?): String?

* Sử dụng webView lấy url chuyển hướng
java.webViewGetOverrideUrl(html: String?, url: String?, js: String?, overrideUrlRegex: String): String?

* Sử dụng webView lấy url tài nguyên
java.webViewGetSource(html: String?, url: String?, js: String?, sourceRegex: String): String?

* Sử dụng trình duyệt tích hợp mở liên kết, có thể dùng để lấy mã xác minh thủ công xác minh trang web chống thu thập dữ liệu
* @param url Liên kết cần mở
* @param title Tiêu đề trình duyệt
java.startBrowser(url: String, title: String)

* Sử dụng trình duyệt tích hợp mở liên kết, và đợi kết quả trang web .body() lấy nội dung trang web
java.startBrowserAwait(url: String, title: String, refetchAfterSuccess: Boolean? = true): StrResponse
```
* Gỡ lỗi
```js
java.log(msg)
java.logType(var)
```
* Lấy mã xác minh do người dùng nhập
```js
java.getVerificationCode(imageUrl)
```
* Thông báo popup
```js
java.longToast(msg: Any?)
java.toast(msg: Any?)
```
* Từ mạng (do java.cacheFile thực hiện), đọc tệp JavaScript cục bộ, nhập ngữ cảnh vui lòng `eval(String(...))` thủ công
```js
java.importScript(url)
//Đường dẫn tương đối hỗ trợ android/data/{package}/cache
java.importScript(relativePath)
java.importScript(absolutePath)
```
* Lưu bộ nhớ đệm tệp mạng
```js
Lấy
java.cacheFile(url)
java.cacheFile(url,saveTime)
Thực thi nội dung
eval(String(java.cacheFile(url)))
Làm vô hiệu bộ nhớ đệm
cache.delete(java.md5Encode16(url))
```
* Lấy dữ liệu đường dẫn chỉ định trong tệp nén mạng *Có thể thay thế Zip Rar 7Z
```js
java.get*StringContent(url: String, path: String): String

java.get*StringContent(url: String, path: String, charsetName: String): String

java.get*ByteArrayContent(url: String, path: String): ByteArray?

```
* Mã hóa URI
```js
java.encodeURI(str: String) //mặc định enc="UTF-8"
java.encodeURI(str: String, enc: String)
```
* base64
> tham số flags có thể bỏ qua, mặc định Base64.NO_WRAP, xem [giải thích tham số flags](https://blog.csdn.net/zcmain/article/details/97051870)
```js
java.base64Decode(str: String)
java.base64Decode(str: String, charset: String)
java.base64DecodeToByteArray(str: String, flags: Int)
java.base64Encode(str: String, flags: Int)
```
* ByteArray
```js
Str chuyển sang Bytes
java.strToBytes(str: String)
java.strToBytes(str: String, charset: String)
Bytes chuyển sang Str
java.bytesToStr(bytes: ByteArray)
java.bytesToStr(bytes: ByteArray, charset: String)
```
* Hex
```js
HexString giải mã thành mảng byte
java.hexDecodeToByteArray(hex: String)
hexString giải mã thành utf8String
java.hexDecodeToString(hex: String)
utf8 mã hóa thành hexString
java.hexEncodeToString(utf8: String)
```
* ID định danh
```js
java.randomUUID()
java.androidId()
```
* Chuyển đổi Phồn/Giản
```js
Chuyển đổi văn bản sang giản thể
java.t2s(text: String): String
Chuyển đổi văn bản sang phồn thể
java.s2t(text: String): String
```
* Định dạng thời gian
```js
java.timeFormatUTC(time: Long, format: String, sh: Int): String?
java.timeFormat(time: Long): String
```
* Định dạng html
```js
java.htmlFormat(str: String): String
```
* Tệp
>  Tất cả các thao tác đọc ghi xóa đối với tệp đều là đường dẫn tương đối, chỉ có thể thao tác tệp trong bộ nhớ đệm Legado/android/data/{package}/cache/
```js
//Tải xuống tệp url dùng để tạo tên tệp, trả về đường dẫn tệp
downloadFile(url: String): String
//Giải nén tệp, zipPath là đường dẫn tệp nén, trả về đường dẫn giải nén
unArchiveFile(zipPath: String): String
unzipFile(zipPath: String): String
unrarFile(zipPath: String): String
un7zFile(zipPath: String): String
//Đọc tất cả tệp trong thư mục
getTxtInFolder(unzipPath: String): String
//Đọc tệp văn bản
readTxtFile(path: String): String
//Xóa tệp
deleteFile(path: String) 
```

### Một số hàm [Lớp mã hóa/giải mã js](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/help/JsEncodeUtils.kt)

> Cung cấp hàm gọi nhanh thuật toán crypto trong môi trường JavaScript, được thực hiện bởi [hutool-crypto](https://www.hutool.cn/docs/#/crypto/概述)  
> Do vấn đề tương thích, phiên bản hutool-crypto hiện tại là 5.8.22  

> Lưu ý: Nếu tham số đầu vào không phải là Utf8String, hãy gọi `java.hexDecodeToByteArray java.base64DecodeToByteArray` để chuyển thành ByteArray trước
* Mã hóa đối xứng
> Tham số đầu vào key iv hỗ trợ ByteArray|**Utf8String**
```js
// Tạo Cipher
java.createSymmetricCrypto(transformation, key, iv)
```
>Giải mã tham số mã hóa data hỗ trợ ByteArray|Base64String|HexString|InputStream
```js
//Giải mã thành ByteArray String
cipher.decrypt(data)
cipher.decryptStr(data)
//Mã hóa thành ByteArray Base64 char HEX char
cipher.encrypt(data)
cipher.encryptBase64(data)
cipher.encryptHex(data)
```
* Mã hóa bất đối xứng
> Tham số đầu vào key hỗ trợ ByteArray|**Utf8String**
```js
//Tạo cipher
java.createAsymmetricCrypto(transformation)
//Thiết lập khóa
.setPublicKey(key)
.setPrivateKey(key)

```
> Giải mã tham số mã hóa data hỗ trợ ByteArray|Base64String|HexString|InputStream  
```js
//Giải mã thành ByteArray String
cipher.decrypt(data,  usePublicKey: Boolean? = true
)
cipher.decryptStr(data, usePublicKey: Boolean? = true
)
//Mã hóa thành ByteArray Base64 char HEX char
cipher.encrypt(data,  usePublicKey: Boolean? = true
)
cipher.encryptBase64(data,  usePublicKey: Boolean? = true
)
cipher.encryptHex(data,  usePublicKey: Boolean? = true
)
```
* Chữ ký
> Tham số đầu vào key hỗ trợ ByteArray|**Utf8String**
```js
//Tạo Sign
java.createSign(algorithm)
//Thiết lập khóa
.setPublicKey(key)
.setPrivateKey(key)
```
> Tham số chữ ký data hỗ trợ ByteArray|inputStream|String
```js
//Chữ ký đầu ra ByteArray HexString
sign.sign(data)
sign.signHex(data)
```
* Tóm tắt (Digest)
```js
java.digestHex(data: String, algorithm: String,): String?

java.digestBase64Str(data: String, algorithm: String,): String?
```
* md5
```js
java.md5Encode(str)
java.md5Encode16(str)
```
* HMac
```js
java.HMacHex(data: String, algorithm: String, key: String): String

java.HMacBase64(data: String, algorithm: String, key: String): String
```

## Các thuộc tính có sẵn của đối tượng book
### Thuộc tính
> Cách sử dụng: Trong js hoặc {{}} sử dụng cách book.thuộc_tính là có thể lấy được. Ví dụ sau nội dung chính thêm ##{{book.name+"Chính văn quyển"+title}} có thể làm sạch các ký tự kiểu Tên sách+Chính văn quyển+Tên chương (như Tôi là đại minh tinh Chính văn quyển Chương 2 Bố tôi là tổng tài hào môn).
```js
bookUrl // Url trang chi tiết (Đường dẫn tệp đầy đủ lưu trữ nguồn sách cục bộ)
tocUrl // Url trang mục lục (toc=table of Contents)
origin // URL nguồn sách (Mặc định BookType.local)
originName //Tên nguồn sách hoặc tên tệp sách cục bộ
name // Tên sách (lấy từ nguồn)
author // Tên tác giả (lấy từ nguồn)
kind // Thông tin phân loại (lấy từ nguồn)
customTag // Thông tin phân loại (người dùng sửa đổi)
coverUrl // Url bìa (lấy từ nguồn)
customCoverUrl // Url bìa (người dùng sửa đổi)
intro // Nội dung giới thiệu (lấy từ nguồn)
customIntro // Nội dung giới thiệu (người dùng sửa đổi)
charset // Tên bộ ký tự tùy chỉnh (chỉ áp dụng cho sách cục bộ)
type // 0:text 1:audio
group // Số chỉ mục nhóm tùy chỉnh
latestChapterTitle // Tiêu đề chương mới nhất
latestChapterTime // Thời gian cập nhật tiêu đề chương mới nhất
lastCheckTime // Thời gian cập nhật thông tin sách gần đây nhất
lastCheckCount // Số lượng chương mới phát hiện gần đây nhất
totalChapterNum // Tổng số mục lục sách
durChapterTitle // Tên chương hiện tại
durChapterIndex // Chỉ mục chương hiện tại
durChapterPos // Tiến độ đọc hiện tại (vị trí chỉ mục của ký tự dòng đầu tiên)
durChapterTime // Thời gian đọc sách gần đây nhất (thời gian mở nội dung)
canUpdate // Cập nhật thông tin sách khi làm mới kệ sách
order // Sắp xếp thủ công
originOrder //Sắp xếp nguồn sách
variable // Thông tin biến sách tùy chỉnh (dùng để truy xuất thông tin sách trong quy tắc nguồn sách)
 ```

## Một số thuộc tính có sẵn của đối tượng chapter
> Cách sử dụng: Trong js hoặc {{}} sử dụng cách chapter.thuộc_tính là có thể lấy được. Ví dụ sau nội dung chính thêm ##{{chapter.title+chapter.index}} có thể làm sạch các ký tự kiểu Tiêu đề chương+Số thứ tự (như Chương 2 Thiên tiên hạ phàm 2).
 ```js
 url // Địa chỉ chương
 title // Tiêu đề chương
 baseUrl //Dùng để nối url tương đối
 bookUrl // Địa chỉ sách
 index // Số thứ tự chương
 resourceUrl // URL thực của âm thanh
 tag //
 start // Vị trí bắt đầu chương
 end // Vị trí kết thúc chương
 variable //Biến
 ```
 
## Một số hàm có sẵn của đối tượng source
* Lấy url nguồn sách
```js
source.getKey()
```
* Truy cập biến nguồn sách
```js
source.setVariable(variable: String?)
source.getVariable()
```

* Thao tác tiêu đề đăng nhập
```js
Lấy tiêu đề đăng nhập
source.getLoginHeader()
Lấy một giá trị khóa của tiêu đề đăng nhập
source.getLoginHeaderMap().get(key: String)
Lưu tiêu đề đăng nhập
source.putLoginHeader(header: String)
Xóa tiêu đề đăng nhập
source.removeLoginHeader()
```
* Thao tác thông tin đăng nhập người dùng
> Sử dụng quy tắc `UI đăng nhập`, và đăng nhập thành công, Legado tự động mã hóa lưu thông tin trong quy tắc UI đăng nhập ngoại trừ thông tin có type là button
```js
Hàm login lấy thông tin đăng nhập
source.getLoginInfo()
Hàm login lấy giá trị khóa thông tin đăng nhập
source.getLoginInfoMap().get(key: String)
Xóa thông tin đăng nhập
source.removeLoginInfo()
```
## Một số hàm có sẵn của đối tượng cookie
```js
Lấy tất cả cookie
cookie.getCookie(url)
Lấy một giá trị khóa cookie
cookie.getKey(url,key)
Đặt cookie
cookie.setCookie(url,cookie)
Thay thế cookie
cookie.replaceCookie(url,cookie)
Xóa cookie
cookie.removeCookie(url)
```

## Một số hàm có sẵn của đối tượng cache
> Đơn vị saveTime: giây, có thể bỏ qua  
> Lưu vào cơ sở dữ liệu và tệp bộ nhớ đệm (50M), khi nội dung lưu lớn vui lòng sử dụng `getFile putFile`
```js
Lưu
cache.put(key: String, value: String, saveTime: Int)
Đọc cơ sở dữ liệu
cache.get(key: String): String?
Xóa
cache.delete(key: String)
Lưu nội dung tệp bộ nhớ đệm
cache.putFile(key: String, value: String, saveTime: Int)
Đọc nội dung tệp
cache.getFile(key: String): String?
Lưu vào bộ nhớ
cache.putMemory(key: String, value: Any)
Đọc bộ nhớ
cache.getFromMemory(key: String): Any?
Xóa bộ nhớ
cache.deleteMemory(key: String)
```

## Hàm chuyển hướng liên kết ngoài/ứng dụng
```js
// Chuyển hướng liên kết ngoài, truyền vào liên kết http hoặc scheme để chuyển đến trình duyệt hoặc ứng dụng khác
java.openUrl(url:String)
// Chỉ định mimeType, có thể chuyển đến ứng dụng loại chỉ định, ví dụ (video/*)
java.openUrl(url:String,mimeType:String)