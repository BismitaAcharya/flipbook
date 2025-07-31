package com.example.flipbook;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@SpringBootApplication
@RestController
@RequestMapping("/api/pdf")
public class FlipbookApplication implements WebMvcConfigurer {

    private static final String IMAGE_DIR = "flipbook-images";

    public static void main(String[] args) {
        SpringApplication.run(FlipbookApplication.class, args);
    }

    @PostConstruct
    public void init() throws IOException {
        Path path = Paths.get(IMAGE_DIR);
        if (Files.exists(path)) {
            FileSystemUtils.deleteRecursively(path);
        }
        Files.createDirectories(path);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,Object> uploadPdf(@RequestParam("file") MultipartFile file) throws IOException {
        if(!"application/pdf".equalsIgnoreCase(file.getContentType())){
            throw new IllegalArgumentException("Uploaded file is not a PDF");
        }
        String pdfFileName = UUID.randomUUID() + ".pdf";
        Path pdfPath = Paths.get(IMAGE_DIR, pdfFileName);
        Files.write(pdfPath, file.getBytes());
        List<String> pages = convertPdfToImages(pdfPath);
        return Collections.singletonMap("pages", pages);
    }

    private List<String> convertPdfToImages(Path pdfPath) throws IOException {
        List<String> imageUrls = new ArrayList<>();
        try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int numPages = doc.getNumberOfPages();
            for (int i = 0; i < numPages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 150);
                String imageName = "page-" + UUID.randomUUID() + "-" + i + ".png";
                Path imagePath = Paths.get(IMAGE_DIR, imageName);
                ImageIO.write(image, "PNG", imagePath.toFile());
                imageUrls.add("/" + IMAGE_DIR + "/" + imageName);
            }
        }
        return imageUrls;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/" + IMAGE_DIR + "/**")
                .addResourceLocations("file:" + IMAGE_DIR + "/");
    }
}
