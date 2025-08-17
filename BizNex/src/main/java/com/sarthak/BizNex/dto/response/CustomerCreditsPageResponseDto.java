package com.sarthak.BizNex.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Paged customers with credits plus aggregate totals (sum & average across all customers with credits > 0, independent of current page)")
public class CustomerCreditsPageResponseDto<T> {
    @Schema(description = "Page content list")
    private List<T> content;
    @Schema(description = "Zero-based page index")
    private int page;
    @Schema(description = "Page size")
    private int size;
    @Schema(description = "Total matching elements (credits > 0 filter applied for this listing)")
    private long totalElements;
    @Schema(description = "Total pages")
    private int totalPages;
    @Schema(description = "Is this the last page")
    private boolean last;
    @Schema(description = "Sum of credits over ALL customers with credits > 0 (not just page)")
    private double totalCredits;
    @Schema(description = "Average credits over ALL customers with credits > 0 (not just page)")
    private double averageCredits;

    public static <T> CustomerCreditsPageResponseDto<T> from(Page<T> page, double totalCredits, double averageCredits){
        return CustomerCreditsPageResponseDto.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .totalCredits(totalCredits)
                .averageCredits(averageCredits)
                .build();
    }
}

