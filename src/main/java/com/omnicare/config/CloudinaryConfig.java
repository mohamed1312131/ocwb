package com.omnicare.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(@Value("${cloudinary.url:}") String cloudinaryUrl) {
        if (StringUtils.hasText(cloudinaryUrl)) {
            Cloudinary cloudinary = new Cloudinary(cloudinaryUrl.trim());
            cloudinary.config.secure = true;
            return cloudinary;
        }

        return new Cloudinary(ObjectUtils.asMap("secure", true));
    }
}
