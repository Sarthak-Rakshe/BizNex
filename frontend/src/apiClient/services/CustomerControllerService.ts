/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CustomerDto } from '../models/CustomerDto';
import type { PageResponseDtoCustomerDto } from '../models/PageResponseDtoCustomerDto';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class CustomerControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param page
     * @param size
     * @param sort
     * @returns PageResponseDtoCustomerDto OK
     * @throws ApiError
     */
    public getAllCustomers(
        page?: number,
        size: number = 20,
        sort: string = 'customerId,asc',
    ): CancelablePromise<PageResponseDtoCustomerDto> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/customers',
            query: {
                'page': page,
                'size': size,
                'sort': sort,
            },
        });
    }
    /**
     * @param requestBody
     * @returns CustomerDto OK
     * @throws ApiError
     */
    public updateCustomer(
        requestBody: CustomerDto,
    ): CancelablePromise<CustomerDto> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/customers',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @param requestBody
     * @returns CustomerDto OK
     * @throws ApiError
     */
    public addCustomer(
        requestBody: CustomerDto,
    ): CancelablePromise<CustomerDto> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/customers',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @param requestBody
     * @returns string OK
     * @throws ApiError
     */
    public addMultipleCustomers(
        requestBody: Array<CustomerDto>,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/customers/bulk',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @param id
     * @returns CustomerDto OK
     * @throws ApiError
     */
    public getCustomerById(
        id: number,
    ): CancelablePromise<CustomerDto> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/customers/{id}',
            path: {
                'id': id,
            },
        });
    }
    /**
     * @param page
     * @param size
     * @param sort
     * @returns PageResponseDtoCustomerDto OK
     * @throws ApiError
     */
    public getCustomersWithCreditsPaged(
        page?: number,
        size: number = 20,
        sort: string = 'customerId,asc',
    ): CancelablePromise<PageResponseDtoCustomerDto> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/customers/credits',
            query: {
                'page': page,
                'size': size,
                'sort': sort,
            },
        });
    }
    /**
     * @param contact
     * @returns CustomerDto OK
     * @throws ApiError
     */
    public getCustomerByContact(
        contact: string,
    ): CancelablePromise<CustomerDto> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/customers/contact/{contact}',
            path: {
                'contact': contact,
            },
        });
    }
    /**
     * @param contact
     * @returns CustomerDto OK
     * @throws ApiError
     */
    public deleteCustomerById(
        contact: string,
    ): CancelablePromise<CustomerDto> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/customers/{contact}',
            path: {
                'contact': contact,
            },
        });
    }
}
