# Trợ giúp quy tắc nguồn

* [Hướng dẫn quy tắc Legado 3.0](https://mgz0227.github.io/The-tutorial-of-Legado/)
* [Tài liệu trợ giúp nguồn sách](https://mgz0227.github.io/The-tutorial-of-Legado/Rule/source.html)
* [Tài liệu trợ giúp nguồn đăng ký](https://mgz0227.github.io/The-tutorial-of-Legado/Rule/rss.html)
* Trong bàn phím hỗ trợ ❓ có thể chèn mẫu tham số URL, mở trợ giúp, hướng dẫn js, hướng dẫn regex, chọn tệp
* Dấu hiệu quy tắc, trong {{......}} sử dụng quy tắc phải có dấu hiệu quy tắc rõ ràng, không có dấu hiệu quy tắc sẽ được coi là js để thực thi
```
@@ Quy tắc mặc định, khi viết trực tiếp có thể bỏ qua @@
@XPath: Quy tắc xpath, khi viết trực tiếp bắt đầu bằng // có thể bỏ qua @XPath
@Json: Quy tắc json, khi viết trực tiếp bắt đầu bằng $. có thể bỏ qua @Json
: Quy tắc regex, không thể bỏ qua, chỉ có thể sử dụng trong danh sách sách và danh sách mục lục
```
* jsLib
> Tiêm JavaScript vào công cụ RhinoJs, hỗ trợ hai định dạng, có thể thực hiện [chia sẻ hàm](https://github.com/gedoor/legado/wiki/JavaScript%E5%87%BD%E6%95%B0%E5%85%B1%E7%94%A8)

> `JavaScript Code` Điền trực tiếp đoạn mã JavaScript
> `{"example":"https://www.example.com/js/example.js", ...}` Tự động sử dụng lại tệp js đã tải xuống

> Lưu ý các hàm được định nghĩa ở đây có thể được gọi bởi nhiều luồng cùng một lúc, nội dung biến toàn cục trong hàm sẽ được chia sẻ, việc sửa đổi chúng có thể gây ra vấn đề cạnh tranh
> Trong hàm không thể khai báo biến toàn cục, biến toàn cục bên ngoài hàm không thể gán lại, nếu không sẽ ném ra ngoại lệ `Không thể sửa đổi thuộc tính của đối tượng bị niêm phong`

* Tỷ lệ đồng thời
> Giới hạn đồng thời, đơn vị ms, có thể điền hai định dạng

> `1000` Khoảng cách truy cập 1s
> `20/60000` Số lần truy cập trong 60s là 20

* Loại nguồn sách: Tệp
> Đối với các trang web cung cấp tải xuống tệp tích hợp như Zhixuan Zangshu, bạn có thể lấy liên kết tệp trong quy tắc URL tải xuống của chi tiết nguồn sách

> Lấy thông tin tệp bằng cách chặn liên kết tải xuống hoặc tiêu đề phản hồi tệp, nếu thất bại sẽ tự động nối `Tên sách` `Tác giả` và trường `type` của `UrlOption` của liên kết tải xuống

> Bộ nhớ cache giải nén tệp nén sẽ được tự động xóa sau lần khởi động tiếp theo, không chiếm thêm không gian

* CookieJar
> Sau khi bật, giá trị trong Set-Cookie của mỗi tiêu đề trả về sẽ được tự động lưu, áp dụng cho các trang web cần session như mã xác minh hình ảnh

* UI đăng nhập
> Không sử dụng webView tích hợp để đăng nhập trang web, cần sử dụng quy tắc `URL đăng nhập` để thực hiện logic đăng nhập, có thể sử dụng `JS kiểm tra đăng nhập` để kiểm tra kết quả đăng nhập
> Thay đổi quan trọng phiên bản 20221113: Nút hỗ trợ gọi hàm trong quy tắc `URL đăng nhập`, bắt buộc phải thực hiện hàm `login`
```
Ví dụ điền quy tắc
[
    {
        "name": "telephone",
        "type": "text"
    },
    {
        "name": "password",
        "type": "password"
    },
    {
        "name": "Đăng ký",
        "type": "button",
        "action": "http://www.yooike.com/xiaoshuo/#/register?title=%E6%B3%A8%E5%86%8C"
    },
    {
        "name": "Lấy mã xác minh",
        "type": "button",
        "action": "getVerificationCode()",
        "style": {
            "layout_flexGrow": 0,
            "layout_flexShrink": 1,
            "layout_alignSelf": "auto",
            "layout_flexBasisPercent": -1,
            "layout_wrapBefore": false
        }
    }
]
```
* URL đăng nhập
> Có thể điền liên kết đăng nhập hoặc JavaScript thực hiện logic đăng nhập của UI đăng nhập
```
Ví dụ điền
function login() {
    java.log("Mô phỏng yêu cầu đăng nhập");
    java.log(source.getLoginInfoMap());
}
function getVerificationCode() {
    java.log("Nút UI đăng nhập: Lấy được số điện thoại"+result.get("telephone"))
}

Hàm nút đăng nhập lấy thông tin đăng nhập
result.get("telephone")
Hàm login lấy thông tin đăng nhập
source.getLoginInfo()
source.getLoginInfoMap().get("telephone")
Các phương thức liên quan đến đăng nhập source, có thể gọi qua source. trong js, có thể tham khảo đăng nhập giọng nói Aliyun
login()
getHeaderMap(hasLoginHeader: Boolean = false)
getLoginHeader(): String?
getLoginHeaderMap(): Map<String, String>?
putLoginHeader(header: String)
removeLoginHeader()
setVariable(variable: String?)
getVariable(): String?
Các hàm liên quan đến AnalyzeUrl, gọi qua java. trong js
initUrl() // Phân tích lại url, có thể dùng để phân tích lại url truy cập lại sau khi đăng nhập bằng js kiểm tra đăng nhập
getHeaderMap().putAll(source.getHeaderMap(true)) // Đặt lại tiêu đề đăng nhập
getStrResponse( jsStr: String? = null, sourceRegex: String? = null) // Trả về kết quả truy cập, loại văn bản, sau khi đăng nhập lại bên trong nguồn sách có thể gọi phương thức này để trả về kết quả lại
getResponse(): Response // Trả về kết quả truy cập, công cụ đọc mạng sử dụng cái này, sau khi gọi đăng nhập có thể gọi phương thức này để truy cập lại, tham khảo kiểm tra đăng nhập Aliyun
```

* Quy tắc danh sách sách (bookList)
* Quy tắc tên sách (name)
* Quy tắc tác giả (author)
* Quy tắc phân loại (kind)
* Quy tắc số từ (wordCount)
* Quy tắc chương mới nhất (lastChapter)
* Quy tắc giới thiệu (intro)
* Quy tắc bìa (coverUrl)
* Quy tắc url trang chi tiết (bookUrl) // URL nguồn sách (Mặc định BookType.local)
* originName // Tên nguồn sách hoặc tên tệp sách cục bộ
* name // Tên sách (lấy từ nguồn)
* author // Tên tác giả (lấy từ nguồn)
* kind // Thông tin phân loại (lấy từ nguồn)
* customTag // Thông tin phân loại (người dùng sửa đổi)
* coverUrl // Url bìa (lấy từ nguồn)
* customCoverUrl // Url bìa (người dùng sửa đổi)
* intro // Nội dung giới thiệu (lấy từ nguồn)
* customIntro // Nội dung giới thiệu (người dùng sửa đổi)
* charset // Tên bộ ký tự tùy chỉnh (chỉ áp dụng cho sách cục bộ)
* type // 0:text 1:audio
* group // Số chỉ mục nhóm tùy chỉnh
* latestChapterTitle // Tiêu đề chương mới nhất
* latestChapterTime // Thời gian cập nhật tiêu đề chương mới nhất
* lastCheckTime // Thời gian cập nhật thông tin sách gần đây nhất
* lastCheckCount // Số lượng chương mới phát hiện gần đây nhất
* totalChapterNum // Tổng số mục lục sách
* durChapterTitle // Tên chương hiện tại
* durChapterIndex // Chỉ mục chương hiện tại
* durChapterPos // Tiến độ đọc hiện tại (vị trí chỉ mục của ký tự dòng đầu tiên)
* durChapterTime // Thời gian đọc sách gần đây nhất (thời gian mở nội dung)
* canUpdate // Cập nhật thông tin sách khi làm mới kệ sách
* order // Sắp xếp thủ công
* originOrder // Sắp xếp nguồn sách
* variable // Biến sách tùy chỉnh (dùng để truy xuất thông tin sách trong quy tắc nguồn)

* Định dạng url khám phá
```json
[
  {
    "title": "xxx",
    "url": "",
    "style": {
      "layout_flexGrow": 0,
      "layout_flexShrink": 1,
      "layout_alignSelf": "auto",
      "layout_flexBasisPercent": -1,
      "layout_wrapBefore": false
    }
  }
]
```

* Tiêu đề yêu cầu, hỗ trợ proxy http, cài đặt proxy socks4 socks5
> Lưu ý key của tiêu đề yêu cầu phân biệt chữ hoa chữ thường
> Định dạng đúng User-Agent Referer
> Định dạng sai user-agent referer
```
Proxy socks5
{
  "proxy":"socks5://127.0.0.1:1080"
}
Không hỗ trợ proxy socks cần xác thực
Proxy http
{
  "proxy":"http://127.0.0.1:1080"
}
Hỗ trợ xác thực máy chủ proxy http
{
  "proxy":"http://127.0.0.1:1080@Tên người dùng@Mật khẩu"
}
Lưu ý: Những tiêu đề yêu cầu này là vô nghĩa, sẽ bị bỏ qua
```

* url thêm tham số js, thực thi khi phân tích url, có thể xử lý url khi truy cập url, ví dụ
```
https://www.baidu.com,{"js":"java.headerMap.put('xxx', 'yyy')"}
https://www.baidu.com,{"js":"java.url=java.url+'yyyy'"}
```

* Thêm phương thức js, dùng để chặn chuyển hướng
  * `java.get(urlStr: String, headers: Map<String, String>)`
  * `java.post(urlStr: String, body: String, headers: Map<String, String>)`
* Đối với nguồn chuyển hướng tìm kiếm, có thể sử dụng phương thức này để lấy url sau khi chuyển hướng
```
(() => {
  if(page==1){
    let url='https://www.yooread.net/e/search/index.php,'+JSON.stringify({
    "method":"POST",
    "body":"show=title&tempid=1&keyboard="+key
    });
    return source.put('surl',String(java.connect(url).raw().request().url()));
  } else {
    return source.get('surl')+'&page='+(page-1)
  }
})()
Hoặc
(() => {
  let base='https://www.yooread.net/e/search/';
  if(page==1){
    let url=base+'index.php';
    let body='show=title&tempid=1&keyboard='+key;
    return base+source.put('surl',java.post(url,body,{}).header("Location"));
  } else {
    return base+source.get('surl')+'&page='+(page-1);
  }
})()
```

* Liên kết hình ảnh hỗ trợ sửa đổi headers
```
let options = {
"headers": {"User-Agent": "xxxx","Referrer":baseUrl,"Cookie":"aaa=vbbb;"}
};
'<img src="'+src+","+JSON.stringify(options)+'">'
```

* Sử dụng phân tích phông chữ
> Phương pháp sử dụng, sử dụng trong quy tắc thay thế nội dung, nguyên lý tìm mã tương ứng với glyph trong f2 dựa trên dữ liệu glyph của phông chữ f1
```
<js>
(function(){
  var b64=String(src).match(/ttf;base64,([^\)]+)/);
  if(b64){
    var f1 = java.queryTTF(b64[1]);
    var f2 = java.queryTTF("https://alanskycn.gitee.io/teachme/assets/font/Source Han Sans CN Regular.ttf");
    // return java.replaceFont(result, f1, f2);
    return java.replaceFont(result, f1, f2, true); // Lọc bỏ các glyph không tồn tại trong f1
  }
  return result;
})()
</js>
```

* Thao tác mua
> Có thể điền trực tiếp liên kết hoặc JavaScript, nếu kết quả thực thi là liên kết mạng sẽ tự động mở trình duyệt, js trả về true tự động làm mới mục lục và chương hiện tại

* Giải mã hình ảnh
> Áp dụng cho trường hợp hình ảnh cần giải mã lần hai, điền trực tiếp JavaScript, trả về `ByteArray` sau khi giải mã
> Giải thích một số biến: java (chỉ hỗ trợ [lớp mở rộng js](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/help/JsExtensions.kt)), result là `ByteArray` của hình ảnh cần giải mã, src là liên kết hình ảnh

```js
java.createSymmetricCrypto("AES/CBC/PKCS5Padding", key, iv).decrypt(result)
```

```js
function decodeImage(data, key) {
  var input = new Packages.java.io.ByteArrayInputStream(data)
  var out = new Packages.java.io.ByteArrayOutputStream()
  var byte
  while ((byte = input.read()) != -1) {
    out.write(byte ^ key)
  }
  return out.toByteArray()
}

decodeImage(result, key)
```

* Giải mã bìa
> Giống giải mã hình ảnh, trong đó result là `inputStream` của bìa cần giải mã

```js
java.createSymmetricCrypto("AES/CBC/PKCS5Padding", key, iv).decrypt(result)
```

```js
function decodeImage(data, key) {
  var out = new Packages.java.io.ByteArrayOutputStream()
  var byte
  while ((byte = data.read()) != -1) {
    out.write(byte ^ key)
  }
  return out.toByteArray()
}

decodeImage(result, key)
```
