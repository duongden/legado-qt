# Giải thích chi tiết biểu thức đường dẫn xpath

_Lưu ý: Tất cả mã trong bài này đều đã được xác minh qua Chrome (phiên bản 123.0.6312.86)_

> Trong đặc tả XPath định nghĩa 13 trục (axes) khác nhau.  
> Trục biểu thị mối quan hệ với phần tử, và được dùng để định vị phần tử trên cây phần tử so với phần tử đó.

-   `namespace` (không hỗ trợ)
-   `attribute` Thuộc tính của phần tử. Có thể viết tắt là `@`
-   `self` Biểu thị bản thân phần tử. Có thể viết tắt là `.`
-   `parent` Phần tử cha của phần tử hiện tại. Có thể viết tắt là `..`
-   `child` Phần tử con của phần tử hiện tại.
-   `ancestor` Tất cả tổ tiên trực tiếp của phần tử hiện tại.
-   `ancestor-or-self` Phần tử hiện tại và tất cả tổ tiên trực tiếp của nó.
-   `descendant` Tất cả phần tử con đệ quy của phần tử hiện tại.
-   `descendant-or-self` Phần tử hiện tại và tất cả phần tử con đệ quy của nó.
-   `following` Tất cả phần tử xuất hiện sau phần tử hiện tại. Bỏ qua cấp độ phần tử, nhưng không bao gồm hậu duệ trực tiếp.
-   `following-sibling` Tất cả phần tử cùng cấp xuất hiện sau phần tử hiện tại.
-   `preceding` Tất cả phần tử xuất hiện trước phần tử hiện tại. Bỏ qua cấp độ phần tử, nhưng không bao gồm tổ tiên trực tiếp.
-   `preceding-sibling` Tất cả phần tử cùng cấp xuất hiện trước phần tử hiện tại.

```js
// Cách dùng trục -> Tên trục::Biểu thức
// Ví dụ:
> $x('//body/ancestor-or-self::*')
< [body, html]
```

#### I. Định dạng cơ bản của biểu thức xpath

> xpath chọn phần tử thông qua "biểu thức đường dẫn" (Path Expression).  
> Về hình thức, "biểu thức đường dẫn" rất giống với hệ thống tệp truyền thống.

```txt
# Dấu gạch chéo "/" làm dấu phân cách bên trong đường dẫn.
# Cùng một phần tử có hai cách viết là đường dẫn tuyệt đối và đường dẫn tương đối.
# Đường dẫn tuyệt đối phải bắt đầu bằng "/", theo sau là phần tử gốc, ví dụ /step/step/...
# Đường dẫn tương đối là các cách viết khác ngoài đường dẫn tuyệt đối, ví dụ step/step, tức là không bắt đầu bằng "/".
# "." biểu thị phần tử hiện tại.
# ".." biểu thị phần tử cha của phần tử hiện tại
```

### II. Quy tắc cơ bản chọn phần tử

```txt
- "/"：Biểu thị chọn phần tử gốc
- "//"：Biểu thị chọn một phần tử ở vị trí bất kỳ
- nodename：Biểu thị chọn phần tử có tên chỉ định
- "@"： Biểu thị chọn một thuộc tính nào đó
```

### III. Ví dụ chọn phần tử

```html
<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8" />
        <title>Tiêu đề</title>
        <meta property="author" content="Tác giả" />
    </head>
    <body>
        <div>
            <title lang="eng">Harry Potter</title>
            <p>29.39</p>
            <p>usd</p>
        </div>
        <div>
            <title lang="cn">Lập trình Cpp nâng cao</title>
            <p>39.95</p>
            <p>rmb</p>
        </div>
        <div id="list">
            <dl>
                <dd><a href="/1">Một</a></dd>
                <dd><a href="/2">Hai</a></dd>
                <dd><a href="/3">Ba</a></dd>
            </dl>
        </div>
    </body>
</html>
```

```js
// Ví dụ 1
> $x('/') // Chọn phần tử gốc, trả về mảng chứa phần tử được chọn.
< [document]
// Ví dụ 2
> $x('/html') // Chọn tất cả phần tử con html dưới phần tử gốc, đây là cách viết đường dẫn tuyệt đối.
< [html]
// Ví dụ 3
> $x('html/head/meta') // Chọn tất cả phần tử meta dưới phần tử head, đây là cách viết đường dẫn tương đối.
< [meta, meta]  // <meta charset="utf-8">, <meta property="author" content="Tác giả">
// Ví dụ 4
> $x('//p') // Chọn tất cả phần tử p, bất kể chúng ở đâu
< [p, p, p, p] // <p>29.39</p>, <p>usd</p>, <p>39.95</p>, <p>rmb</p>
// Ví dụ 5
> $x('html/body//a') // Chọn tất cả phần tử a dưới phần tử body
< [a, a, a] // <a href="/1">Một</a>, <a href="/2">Hai</a>, <a href="/3">Ba</a>
// Ví dụ 6
> $x('//@lang') // Chọn tất cả thuộc tính có tên là lang.
< [lang, lang] // lang="eng", lang="cn"
> $x('html/head/meta/@content') // Chọn thuộc tính content của tất cả phần tử meta dưới phần tử head.
< [content] // content="Tác giả"
// Ví dụ 7
> $x('//meta/..') // Chọn phần tử cha của tất cả phần tử meta. (Kết quả giống nhau sẽ chỉ trả về một)
< [head] // <head>...</head>
```

### IV. Điều kiện vị ngữ của xpath (Predicate)

> Cái gọi là "điều kiện vị ngữ", chính là điều kiện bổ sung cho biểu thức đường dẫn.  
> Tất cả các điều kiện bổ sung đều được viết trong dấu ngoặc vuông `[]`, dùng để lọc thêm các phần tử.
> Chỉ các phần tử có kết quả biểu thức trong ngoặc vuông là true mới được chọn.

```js
// Ví dụ 8
> $x('html/head/meta[1]') //  Chọn phần tử meta đầu tiên dưới phần tử head
< [meta] // <meta charset="utf-8">
> $x('//p[1]') // Chọn phần tử p đầu tiên dưới tất cả các phần tử
< [p, p] // <p>29.39</p>, <p>39.95</p>
// Ví dụ 9
> $x('html/head/meta[last()]') // Chọn phần tử meta cuối cùng dưới phần tử head
< [meta] // <meta property="author" content="Tác giả">
// Ví dụ 10
> $x('html/head/meta[last()-1]') // Chọn phần tử meta áp chót dưới phần tử head
< [meta] // <meta charset="utf-8">
// Ví dụ 11
> $x('html/head/meta[position()>1]') // Chọn tất cả phần tử meta dưới phần tử head trừ phần tử đầu tiên
< [meta] // <meta property="author" content="Tác giả">
// Ví dụ 12
> $x('//title[@lang]') // Chọn tất cả phần tử title có thuộc tính lang.
< [title, title] // <title lang="eng">Harry Potter</title>, <title lang="cn">Lập trình Cpp nâng cao</title>
// Ví dụ 13
> $x('//title[@lang="eng"]') // Chọn tất cả phần tử title có giá trị thuộc tính lang bằng "eng".
< [title] // <title lang="eng">Harry Potter</title>
// Ví dụ 14
> $x('/html/body/div[dl]') // Chọn phần tử con div của body, và phần tử div được chọn phải có phần tử con dl.
< [div] // <div id="list"><dl id="list">...</dl></div>
// Ví dụ 15
> $x('/html/body/div[p>35.00]') // Chọn phần tử con div của body, và giá trị phần tử con p của phần tử div được chọn phải lớn hơn 35.00.
< [div] // <div><title lang="cn">Lập trình Cpp nâng cao</title><p>39.95</p><p>rmb</p></div>
> $x('/html/body/div[p="rmb"]') // Chọn phần tử con div của body, và giá trị phần tử con p của phần tử div được chọn phải bằng "rmb".
< [div] // <div><title lang="cn">Lập trình Cpp nâng cao</title><p>39.95</p><p>rmb</p></div>
// Ví dụ 16
> $x('/html/body/div[p="rmb"]/title') // Trong tập kết quả ví dụ 14, chọn phần tử con title.
< [title] // <title lang="cn">Lập trình Cpp nâng cao</title>
// Ví dụ 17
> $x('/html/body/div/p[.>35.00]') // Chọn phần tử con p của "/html/body/div" có giá trị lớn hơn 35.00.
< [p] // <p>39.95</p>
```

### V. Ký tự đại diện

-   `\*` Biểu thị khớp với bất kỳ phần tử nào.
-   `@\*` Biểu thị khớp với bất kỳ tên thuộc tính nào.

```js
// Ví dụ 18
> $x('//*') // Chọn tất cả phần tử, kết quả trả về theo thứ tự đệ quy
< [html, head, meta, title, meta, body, div, title, p, p, div, title, p, p, div, dl, dd, a, dd, a, dd, a]
// Ví dụ 19
> $x('/*/*') // Chọn tất cả phần tử tầng thứ hai
< [head, body] // <head>...</head>, <body>...</body>
// Ví dụ 20
> $x('//dl[@id="list"]/*') // Chọn tất cả phần tử con của phần tử dl có id="list".
< [dd, dd, dd] // <dd><a href="/1">Một</a></dd>, <dd><a href="/2">Hai</a></dd>, <dd><a href="/3">Ba</a></dd>
// Ví dụ 21
> $x('//title[@*]') // Chọn tất cả phần tử title có thuộc tính.
< [title, title] // <title lang="eng">Harry Potter</title>, <title lang="cn">Lập trình Cpp nâng cao</title>
```

### VI. Chọn nhiều đường dẫn

-   Dùng `|` để gộp kết quả chọn của nhiều biểu thức.

```js
// Ví dụ 22
> $x('//title | //a') // Chọn tất cả phần tử title và a.
< [title, title, title, a, a, a]

```

### VII. Hàm của xpath

> Tham số hàm của xpath có thể là chuỗi tĩnh hoặc biểu thức, và hàm có thể gọi lồng nhau.  
> Chỉ mục của xpath bắt đầu từ 1, không phải bắt đầu từ 0.

```js
// boolean(expression) Chuyển đổi kết quả chọn của biểu thức thành giá trị boolean.
> $x('boolean(//title)')
< true
// number([object]) Chuyển đổi kết quả chọn của biểu thức thành số. (Nội dung phần tử HTML mặc định đều là chuỗi)
> $x('number(//p[1])')
< 29.39
// round(decimal) Chuyển đổi tham số số thành số nguyên và làm tròn.
> $x('round(//p[1])')
< 29
// ceiling(number) Chuyển đổi tham số số thành số nguyên và làm tròn lên. ceiling(5.2)=6
> $x('ceiling(//p[1])') // Chỉ sử dụng phần tử đầu tiên khớp với biểu thức
< 30
// floor(number) Chuyển đổi tham số số thành số nguyên và làm tròn xuống. floor(5.8)=5
> $x('floor(//p[1])')
< 29
// concat( string1, string2 [,stringn]* ) Nối chuỗi, tham số là chuỗi tĩnh hoặc biểu thức
> $x('concat("cost:", //p[1], //p[2])') // Chỉ sử dụng phần tử đầu tiên khớp với biểu thức
< 'cost:29.39usd'
// contains(haystack, needle) Kiểm tra haystack có chứa needle không, trả về boolean
> $x('contains(//p[1], "29.39")') // Chỉ sử dụng phần tử đầu tiên khớp với biểu thức
< true
> $x('//title[contains(., "Harry")]') // Chọn phần tử title có nội dung chứa "Harry".
< [title] // <title lang="eng">Harry Potter</title>
// count( node-set ) Thống kê số lượng phần tử được chọn bởi biểu thức.
> $x('count(//p)')
< 4
// id(expression) Chọn phần tử theo thuộc tính id, nếu tham số là biểu thức, sẽ lấy kết quả biểu thức làm id để truy vấn.
> $x('id(//dl/@id)') // Tương đương với $x('id("list")')
< [dl#list] // <dl id="list">...</dl>
// last() Trả về số lượng thành viên của tập hợp phần tử cùng cấp khớp với biểu thức đường dẫn hiện tại.
> $x('//p[last()]')
< [p, p] // <p>usd</p>, <p>rmb</p>
// name([node-set]) Trả về tên phần tử kèm không gian tên của thành viên đầu tiên trong tập hợp được chọn bởi biểu thức, trong HTML tương đương với local-name([node-set]).
// local-name([node-set]) Trả về tên phần tử cục bộ của thành viên đầu tiên trong tập hợp được chọn bởi biểu thức.
> $x('local-name(//*[@id])') //
< 'dl'
// namespace-uri([node-set]) Lấy URI không gian tên của nút đầu tiên trong tập nút đã chọn.
> $x('namespace-uri(//div)')
< 'http://www.w3.org/1999/xhtml' // HTML thường trả về giá trị cố định này
// normalize-space([string]) Loại bỏ khoảng trắng trước và sau nội dung văn bản cũng như thay thế khoảng trắng liên tiếp bên trong thành một dấu cách
> $x('normalize-space("  test    string   ")')
< 'test string'
// not(expression) Trả về giá trị nghịch đảo boolean của biểu thức.
> $x('//title[not(@lang)]')
< [title] // <title>Tiêu đề</title>
// position() Trả về vị trí của phần tử được chọn trong tập hợp phần tử cùng cấp khớp với biểu thức đường dẫn.
> $x('//meta[position()=2]')
< [meta] // <meta property="author" content="Tác giả" />
// starts-with(haystack, needle) Kiểm tra xem chuỗi haystack có bắt đầu bằng chuỗi needle hay không.
> $x('//title[starts-with(., "Cpp")]')
< [title] // <title lang="cn">Lập trình Cpp nâng cao</title]
// string([object]) Chuyển đổi tham số đã cho thành chuỗi
> $x('string(//p)')
< '29.39'
// string-length([string]) Trả về số lượng ký tự của chuỗi đã cho
> $x('string-length(string(//p))')
< 5
// substring(string, start[, length]) Cắt chuỗi
> $x('substring(string(//p), 1, 3)')
< '29.'
// substring-after(haystack, needle) Trả về chuỗi sau needle đầu tiên trong chuỗi haystack.
> $x('substring-after(string(//p), ".")')
< '39'
// substring-before(haystack, needle) Trả về chuỗi trước needle đầu tiên trong chuỗi haystack.
> $x('substring-before(string(//p), ".")')
< '29'
// sum([node-set]) Tính tổng các số trong tập hợp đã cho. Nếu có phần tử không phải số trong tập hợp, trả về NaN
> $x('sum(//p[1])')
< 69.34
// translate(string, "abc", "XYZ") Lần lượt thay thế a, b, c xuất hiện trong string thành X, Y, Z ở vị trí tương ứng.
// Nếu số ký tự trong tham số thứ ba ít hơn tham số thứ hai, thì các ký tự tương ứng trong tham số thứ nhất sẽ bị xóa.
> $x('translate("aabbcc112233", "ac2", "V8")')
< 'VVbb881133'
// true() Biểu thị giá trị boolean true trong hàm
// false() Biểu thị giá trị boolean false trong hàm
```
