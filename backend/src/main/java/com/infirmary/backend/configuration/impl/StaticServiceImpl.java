package com.infirmary.backend.configuration.impl;

import java.io.InputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import com.infirmary.backend.configuration.service.StaticService;

import lombok.NoArgsConstructor;

@Service
@NoArgsConstructor
public class StaticServiceImpl implements StaticService {

    @Override
    public byte[] imageReturn(String filename) {
        try {
            // Loads the image file from src/main/resources/static/Profile
            ClassPathResource imgFile = new ClassPathResource("static/Profile/" + filename);
            InputStream inputStream = imgFile.getInputStream();

            // Convert input stream to byte array for response
            return IOUtils.toByteArray(inputStream);

        } catch (IOException e) {
            e.printStackTrace();
            throw new ResourceNotFoundException("Image Not Found");
        }
    }
}
