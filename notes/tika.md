## Tika
作用： 文档解析工具
官方地址： https://tika.apache.org/ 
## 使用方式
- 注解开发
- 非注解开发
- springBoot中集成

## spring boot集成 Tika

依赖
```xml
<!-- https://mvnrepository.com/artifact/org.apache.tika/tika-core -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>3.2.3</version>
</dependency>
<!-- https://mvnrepository.com/artifact/org.apache.tika/tika-parsers-standard-package -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-parsers-standard-package</artifactId>
    <version>3.2.3</version>
</dependency>
```

创建配置文件tika-config.xml

创建配置类

常用的三个能力
```java
    public String detectFileType(InputStream inputStream) throws IOException{
        return tika.detect(inputStream);//获取文件类型
    }

    public String extractText(InputStream inputStream) throws IOException{
        return tika.parseToString(inputStream);//提取文本以及文件的元数据
    }
```

```bash
2026-01-09T09:07:02.240+08:00  WARN 17264 --- [l-1:housekeeper] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Thread starvation or clock leap detected (housekeeper delta=6h12m37s40ms820µs300ns).
```
这个问题的核心是 程序进入了「无限阻塞 / 死循环」，结合日志中的 Thread starvation or clock leap detected（线程饥饿 / 时钟跳变），说明线程被长时间阻塞，根本不是 “正常运行”。

### 定位阻塞核心
- Tika/PDFBox 解析 PDF 时的资源死锁
- ```java
    // 显式指定 PDF 解析器（提升解析效率，避免自动检测耗时）
    AutoDetectParser parser = new AutoDetectParser();
    parseContext.set(PDFParser.class, new PDFParser());     
    // 关键：禁用 PDF 字体解析（避免特殊字体导致的阻塞）
    PDFParserConfig pdfConfig = new PDFParserConfig();
    pdfConfig.setExtractInlineImages(false); // 不提取图片
    pdfConfig.setFontExtraction(PDFParserConfig.FontExtraction.NONE); // 不解析字体
    parseContext.set(PDFParserConfig.class, pdfConfig);
    ```
- 正则表达式无限回溯（导致 CPU 100% 占用）
- ```java
    // 原正则（易回溯）
    Pattern questionPattern = Pattern.compile(
    "(\\d+)、([\\s\\S]+?)\\((\\s*[A-D]\\s*)\\)\\s*" +
    "A、([\\s\\S]+?)" +
    "B、([\\s\\S]+?)" +
    "C、([\\s\\S]+?)" +
    "D、([\\s\\S]+?)(?=\\d+、|$)"
    );
    
    // 优化后（限制匹配范围，避免回溯）
    Pattern questionPattern = Pattern.compile(
    "(\\d+)、([^\\n]+?)" + // 题干只匹配“不换行的内容”（若题干换行，可改为 [^A、]+?）
    "\\((\\s*[A-D]\\s*)\\)\\s*" +
    "A、([^B、]+?)" + // 选项A只匹配到“B、”前
    "B、([^C、]+?)" + // 选项B只匹配到“C、”前
    "C、([^D、]+?)" + // 选项C只匹配到“D、”前
    "D、([^\\d]+?)(?=\\d+、|$)", // 选项D只匹配到“下一题编号”前
    Pattern.CASE_INSENSITIVE
    );
    ```