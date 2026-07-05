package com.nursery.pdf;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.microsoft.playwright.options.Margin;

@Service
public class ChromiumPdfService {

    public byte[] generatePdf(String html) {

    BrowserContext context = browser.newContext();

    try {
        Page page = context.newPage();

        page.setContent(
                html,
                new Page.SetContentOptions()
                        .setWaitUntil(WaitUntilState.NETWORKIDLE)
        );

        return page.pdf(
                new Page.PdfOptions()
                        .setFormat("A4")
                        .setPrintBackground(true)
                        .setMargin(new Margin()
                                .setTop("15mm")
                                .setBottom("15mm")
                                .setLeft("15mm")
                                .setRight("15mm"))
        );

    } finally {
        context.close();
    }
}

    private Playwright playwright;
    private Browser browser;

    @PostConstruct
    public void init() {

        playwright = Playwright.create();

        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(true)
                        .setArgs(java.util.List.of(
                                "--no-sandbox",
                                "--disable-setuid-sandbox",
                                "--disable-dev-shm-usage"
                        ))
        );
    }

    @PreDestroy
    public void destroy() {

        if (browser != null) {
            browser.close();
        }

        if (playwright != null) {
            playwright.close();
        }
    }

}