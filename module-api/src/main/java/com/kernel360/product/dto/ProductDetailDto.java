package com.kernel360.product.dto;

import com.kernel360.product.entity.Product;

import java.time.LocalDate;

/**
 * DTO for {@link com.kernel360.product.entity.Product}
 */
public record ProductDetailDto(
        Long productNo,
        String productName,
        String barcode,
        String imageSource,
        String reportNumber,
        String safetyStatus,
        Integer viewCount,
        String companyName,
        String productType,
        LocalDate issuedDate,
        String safetyInspectionStandard,
        String upperItem,
        String item,
        String propose,
        String weight,
        String usage,
        String usagePrecaution,
        String firstAid,
        String mainSubstance,
        String allergicSubstance,
        String otherSubstance,
        String preservative,
        String surfactant,
        String fluorescentWhitening,
        String manufactureType,
        String manufactureMethod,
        String manufactureNation,
        String violationInfo
) {

    public static ProductDetailDto of(
            Long   productNo,
            String productName,
            String barcode,
            String imageSource,
            String reportNumber,
            String safetyStatus,
            Integer viewCount,
            String companyName,
            String productType,
            LocalDate issuedDate,
            String safetyInspectionStandard,
            String upperItem,
            String item,
            String propose,
            String weight,
            String usage,
            String usagePrecaution,
            String firstAid,
            String mainSubstance,
            String allergicSubstance,
            String otherSubstance,
            String preservative,
            String surfactant,
            String fluorescentWhitening,
            String manufactureType,
            String manufactureMethod,
            String manufactureNation,
            String violationInfo
    ) {
        return new ProductDetailDto(
                productNo,
                productName,
                barcode,
                imageSource,
                reportNumber,
                safetyStatus,
                viewCount,
                companyName,
                productType,
                issuedDate,
                safetyInspectionStandard,
                upperItem,
                item,
                propose,
                weight,
                usage,
                usagePrecaution,
                firstAid,
                mainSubstance,
                allergicSubstance,
                otherSubstance,
                preservative,
                surfactant,
                fluorescentWhitening,
                manufactureType,
                manufactureMethod,
                manufactureNation,
                violationInfo
        );
    }

    public static ProductDetailDto from(Product entity) {
        return ProductDetailDto.of(
            entity.getProductNo(),
            entity.getProductName(),
            entity.getBarcode(),
            entity.getImageSource(),
            entity.getReportNumber(),
            entity.getSafetyStatus().name(),
            entity.getViewCount(),
            entity.getCompanyName(),
            entity.getProductType(),
            entity.getIssuedDate(),
            entity.getSafetyInspectionStandard(),
            entity.getUpperItem(),
            entity.getItem(),
            entity.getPropose(),
            entity.getWeight(),
            entity.getUsage(),
            entity.getUsagePrecaution(),
            entity.getFirstAid(),
            entity.getMainSubstance(),
            entity.getAllergicSubstance(),
            entity.getOtherSubstance(),
            entity.getPreservative(),
            entity.getSurfactant(),
            entity.getFluorescentWhitening(),
            entity.getManufactureType(),
            entity.getManufactureMethod(),
            entity.getManufactureNation(),
            entity.getViolationInfo()
        );
    }
}