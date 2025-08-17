/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BillDto } from '../models/BillDto';
import type { BillResponseDto } from '../models/BillResponseDto';
import type { PageResponseDtoBillResponseDto } from '../models/PageResponseDtoBillResponseDto';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class BillingControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param page
     * @param size
     * @param sort
     * @returns PageResponseDtoBillResponseDto OK
     * @throws ApiError
     */
    public getAllBills(
        page?: number,
        size: number = 20,
        sort: string = 'billId,asc',
    ): CancelablePromise<PageResponseDtoBillResponseDto> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/billing',
            query: {
                'page': page,
                'size': size,
                'sort': sort,
            },
        });
    }
    /**
     * @param requestBody
     * @returns BillResponseDto OK
     * @throws ApiError
     */
    public createBill(
        requestBody: BillDto,
    ): CancelablePromise<BillResponseDto> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/billing',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @param requestBody
     * @returns BillResponseDto OK
     * @throws ApiError
     */
    public updateBill(
        requestBody: BillDto,
    ): CancelablePromise<BillResponseDto> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/billing/return-bill',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @param requestBody
     * @returns BillResponseDto OK
     * @throws ApiError
     */
    public creditBill(
        requestBody: BillDto,
    ): CancelablePromise<BillResponseDto> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/billing/credit-bill',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @param billNumber
     * @returns BillResponseDto OK
     * @throws ApiError
     */
    public getBillByBillNumber(
        billNumber: string,
    ): CancelablePromise<BillResponseDto> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/billing/{billNumber}',
            path: {
                'billNumber': billNumber,
            },
        });
    }
    /**
     * @param contact
     * @param page
     * @param size
     * @param sort
     * @returns PageResponseDtoBillResponseDto OK
     * @throws ApiError
     */
    public getBillsByCustomerContactPaged(
        contact: string,
        page?: number,
        size: number = 20,
        sort: string = 'billId,asc',
    ): CancelablePromise<PageResponseDtoBillResponseDto> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/billing/customer/{contact}',
            path: {
                'contact': contact,
            },
            query: {
                'page': page,
                'size': size,
                'sort': sort,
            },
        });
    }
    /**
     * @param id
     * @returns any OK
     * @throws ApiError
     */
    public deleteBill(
        id: number,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/billing/{id}',
            path: {
                'id': id,
            },
        });
    }
}
