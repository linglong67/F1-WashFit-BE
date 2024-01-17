package com.kernel360.modulebatch.product.job;

import com.kernel360.brand.entity.Brand;
import com.kernel360.brand.repository.BrandRepository;
import com.kernel360.ecolife.entity.ReportedProduct;
import com.kernel360.ecolife.repository.ReportedProductRepository;
import com.kernel360.modulebatch.product.dto.ProductDto;
import com.kernel360.product.entity.Product;
import com.kernel360.product.repository.ProductRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ImportProductFromReportedProductJobConfig {

    private final ProductRepository productRepository;

    private final ReportedProductRepository reportedProductRepository;

    private final BrandRepository brandRepository;

    private final EntityManagerFactory emf;

    @Bean
    public Job importProductFromReportedProductJob(JobRepository jobRepository,
                                                   @Qualifier("importProductFromReportedProductStep") Step importProductFromReportedProductStep) {
        log.info("Import Product from ReportedProduct by Brand Job Build Configuration");

        return new JobBuilder("importProductFromReportedProductJob", jobRepository)
                .start(importProductFromReportedProductStep)
                .incrementer(new RunIdIncrementer())
                .listener(new ImportProductFromReportedProductListener())
                .build();
    }

    @Bean
    @JobScope
    public Step importProductFromReportedProductStep(JobRepository jobRepository,
                                                     PlatformTransactionManager transactionManager) throws Exception {
        log.info("Import Product from ReportedProduct by Brand Step Build Configuration");

        return new StepBuilder("importProductFromReportedProductStep", jobRepository)
                .<Brand, List<Product>>chunk(10, transactionManager)
                .reader(brandReader())
                .processor(reportedProductToProductListProcessor())
                .writer(productListWriter())
                .build();
    }

    /**
     * @return Brand 를 읽어오는 JpaPagingReader
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<Brand> brandReader() throws Exception {
        JpaPagingItemReader<Brand> itemReader = new JpaPagingItemReader<>();
        itemReader.setPageSize(50);
        itemReader.setEntityManagerFactory(emf);
        itemReader.setQueryString("select b from Brand b");
        itemReader.setName("jpaPagingBrandReader");
        itemReader.afterPropertiesSet();

        return itemReader;
    }

    @Bean
    @StepScope
    public ItemProcessor<Brand, List<Product>> reportedProductToProductListProcessor() throws Exception {
        return brand -> {
            //-- ReportedProduct 테이블에서 브랜드명과 제조사명으로 제품 검색 --//
            List<ReportedProduct> reportedProductList = reportedProductRepository
                    .findByBrandNameAndCompanyName(
                            brand.getCompanyName().replaceAll(" ", "%"),
                            brand.getBrandName().replaceAll(" ", "%")
                    );

//            // TODO :: 브랜드명과 제조사명으로 검색해 온 제품과 중복해서 들어가게 될 수 있으므로 검색 조건을 좀 더 생각해보아야 함
//            // TODO :: 예를 들면 복합키를 만들어서 Unique Key 로 지정하기, 그리고 Unique 키로 유효성을 검증하기, 검색하기
//            //-- ReportedProduct 테이블에서 제조사명으로 제품 검색 (브랜드명이 제품명에 포함되지 않는 경우) --//
//            List<String> brandNamesToExclude = brandRepository.findByCompanyName(brand.getCompanyName())
//                                                              .stream()
//                                                              .map(Brand::getBrandName)
//                                                              .toList();
//            // 같은 회사, 이름이 다른 가장 최신의 제품
//            List<ReportedProduct> reportedProductOfCompany = reportedProductRepository
//                    .findByCompanyName(brand.getCompanyName().replaceAll(" ", "%"))
//                    .stream()
//                    .collect(Collectors.groupingBy(ReportedProduct::getProductName,
//                            Collectors.maxBy(Comparator.comparing(ReportedProduct::getIssuedDate))))
//                    .values().stream()
//                    .filter(Optional::isPresent)
//                    .map(Optional::get)
//                    .toList();
//
//            for (ReportedProduct reportedProduct : reportedProductOfCompany) {
//                boolean excludedBrandInProductName = brandNamesToExclude
//                        .stream()
//                        .anyMatch(brandToExclude -> reportedProduct.getProductName()
//                                                                   .contains(brandToExclude));
//                // false, 즉 해당 브랜드명들을 모두 포함하지 않는 경우에만
//                if (!excludedBrandInProductName) {
//                    Optional<Product> findProduct = productRepository.findProductByProductNameAndReportNumber(
//                            reportedProduct.getProductName(), reportedProduct.getSafetyReportNumber());
//
//                    if (findProduct.isEmpty()) {
//                        reportedProductList.add(reportedProduct);
//                    }
//                }
//            }

            //-- 제품명이 같은 제품끼리 그룹을 짓고, issuedDate 기준으로 내림차순 정렬. 가장 최신의 제품정보 하나씩을 ProductDto 로 변환하여 리스트에 추가 --//
            List<ProductDto> productDtoList = reportedProductList.stream()
                                                                 .collect(Collectors.groupingBy(
                                                                         ReportedProduct::getProductName,
                                                                         Collectors.maxBy(
                                                                                 Comparator.comparing(
                                                                                         ReportedProduct::getIssuedDate))))
                                                                 .values().stream()
                                                                 .filter(Optional::isPresent)
                                                                 .map(Optional::get)
                                                                 .map(rp -> ProductDto.of(rp.getProductName(),
                                                                         "barcode",
                                                                         "description",
                                                                         false,
                                                                         0,
                                                                         rp.getCompanyName(),
                                                                         rp.getSafetyReportNumber(),
                                                                         rp.getProductType(), rp.getSafeStandard()
                                                                         , rp.getUpperItem(), rp.getItem(),
                                                                         rp.getProductPropose(), rp.getWeightAndBulk(),
                                                                         rp.getUseMethod(),
                                                                         rp.getUsageAttentionReport(),
                                                                         rp.getFirstAid(), rp.getMainSubstance(),
                                                                         rp.getAllergicSubstance(),
                                                                         rp.getOtherSubstance(), rp.getPreservative(),
                                                                         rp.getSurfactant(),
                                                                         rp.getFluorescentWhiteningAgent(),
                                                                         rp.getManufacture(), rp.getManufactureMethod(),
                                                                         rp.getManufactureNation(), brand))
                                                                 .collect(Collectors.toList());

            return convertToProductList(productDtoList);
        };
    }


    private List<Product> convertToProductList(List<ProductDto> productDtoList) {
        List<Product> productList = new ArrayList<>();

        for (ProductDto productDto : productDtoList) {
            Optional<Product> existingProduct = productRepository.findProductByProductNameAndReportNumber(
                    productDto.productName(),
                    productDto.reportNumber());

            if (existingProduct.isEmpty()) {
                Product newProduct = Product.of(productDto.productName(), productDto.barcode(),
                        productDto.description(),
                        productDto.reportNumber(), productDto.isViolation(), productDto.viewCount(),
                        productDto.companyName(),
                        productDto.productType(),
                        productDto.safetyInspectionStandard(), productDto.upperItem(), productDto.item(),
                        productDto.propose(), productDto.weight(), productDto.usage(), productDto.usagePrecaution(),
                        productDto.firstAid(), productDto.mainSubstance(), productDto.allergicSubstance(),
                        productDto.otherSubstance(), productDto.preservative(), productDto.Surfactant(),
                        productDto.fluorescentWhitening(), productDto.manufactureType(), productDto.manufactureMethod(),
                        productDto.manufactureCountry(), productDto.brand());

                productList.add(newProduct);
                log.info("New product added : ProductNo = {}, Brand = {}, ProductName = {}",
                        newProduct.getProductNo(), newProduct.getBrand().getBrandName(), newProduct.getProductName());
            }
            // TODO :: 업데이트 될 컬럼이 Product 에 존재하게 되면 이 아래에 추가
        }

        return productList;
    }

    /**
     * List 를 저장하기 위한 JpaListWriter. 컴포지트 패턴과 유사하게 구현함.
     */
    private JpaProductListWriter<Product> productListWriter() {
        JpaItemWriter<Product> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(emf);
        writer.setUsePersist(true);

        return new JpaProductListWriter<>(writer);
    }

    //-- Execution Listener --//

    public static class ImportProductFromReportedProductListener implements JobExecutionListener {
        @Override
        public void beforeJob(JobExecution jobExecution) {
            log.info("{} starts", jobExecution.getJobInstance().getJobName());
        }

        @Override
        public void afterJob(JobExecution jobExecution) {
            log.info("{} ends", jobExecution.getJobInstance().getJobName());
        }
    }

}

