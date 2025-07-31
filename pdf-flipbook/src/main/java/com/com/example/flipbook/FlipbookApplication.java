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
        // Clear and create the directory for storing images
        Path dir = Paths.get(IMAGE_DIR);
        if (Files.exists(dir)) {
            FileSystemUtils.deleteRecursively(dir);
        }
        Files.createDirectories(dir);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadPdf(@RequestParam("file") MultipartFile file) throws IOException {
        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new IllegalArgumentException("Uploaded file is not a PDF");
        }

        // Save uploaded PDF temporarily
        String pdfFilename = UUID.randomUUID() + ".pdf";
        Path pdfPath = Paths.get(IMAGE_DIR, pdfFilename);
        Files.write(pdfPath, file.getBytes());

        // Convert PDF pages to PNG images
        List<String> imageUrls = convertPdfToImages(pdfPath);

        // Return URLs for frontend to load
        return Collections.singletonMap("pages", imageUrls);
    }

    private List<String> convertPdfToImages(Path pdfPath) throws IOException {
        List<String> imageUrls = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 150); // 150 DPI for quality
                String imageName = "page-" + UUID.randomUUID() + "-" + i + ".png";
                Path imagePath = Paths.get(IMAGE_DIR, imageName);
                ImageIO.write(image, "PNG", imagePath.toFile());
                imageUrls.add("/" + IMAGE_DIR + "/" + imageName);
            }
        }
        return imageUrls;
    }

    // Serve images from the filesystem folder under URL /flipbook-images/**
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/" + IMAGE_DIR + "/**")
                .addResourceLocations("file:" + IMAGE_DIR + "/");
    }
}
