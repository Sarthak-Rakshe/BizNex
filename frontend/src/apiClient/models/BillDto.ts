/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BillItemDto } from './BillItemDto';
import type { CustomerDto } from './CustomerDto';
export type BillDto = {
    billNumber?: string;
    customer?: CustomerDto;
    billType?: string;
    billItems?: Array<BillItemDto>;
    paymentMethod?: string;
    billDate?: string;
    billTotalAmount?: number;
    billTotalDiscount?: number;
    billStatus?: string;
    originalBillNumber?: string;
};

