package com.poorvi.url_shortener.url.dto;

import lombok.Data;

@Data
public class CreateUrlRequest {

    private String originalUrl;
}