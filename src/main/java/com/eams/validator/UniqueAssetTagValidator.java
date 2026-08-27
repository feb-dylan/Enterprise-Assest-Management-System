package com.eams.validator;

import com.eams.repository.AssetRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UniqueAssetTagValidator implements ConstraintValidator<UniqueAssetTag, String> {

    private final AssetRepository assetRepository;

    @Override
    public boolean isValid(String assetTag, ConstraintValidatorContext context) {
        if (assetTag == null || assetTag.isBlank()) {
            return true;
        }
        return !assetRepository.existsByAssetTag(assetTag);
    }
}