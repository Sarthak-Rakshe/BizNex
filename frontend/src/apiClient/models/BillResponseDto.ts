/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BillItemResponseDto } from './BillItemResponseDto';
export type BillResponseDto = {
    billNumber?: string;
    billDate?: string;
    customerName?: string;
    customerEmail?: string;
    customerPhone?: string;
    billType?: string;
    billItems?: Array<BillItemResponseDto>;
    paymentMethod?: string;
    totalAmount?: number;
    totalDiscount?: number;
    billStatus?: string;
    originalBillNumber?: string;
};

