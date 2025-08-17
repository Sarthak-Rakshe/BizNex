/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { PageResponseDtoProductDto } from '../models/PageResponseDtoProductDto';
import type { ProductDto } from '../models/ProductDto';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class ProductControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param id
     * @returns ProductDto OK
     * @throws ApiError
     */
    public getProductById(
        id: number,
    ): CancelablePromise<ProductDto> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/products/{id}',
            path: {
                'id': id,
            },
        });
    }
    /**
     * @param id
     * @param requestBody
     * @returns ProductDto OK
     * @throws ApiError
     */
    public updateProduct(
        id: number,
        requestBody: ProductDto,
    ): CancelablePromise<ProductDto> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/products/{id}',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @param id
     * @returns ProductDto OK
     * @throws ApiError
     */
    public deleteProduct(
        id: number,
    ): CancelablePromise<ProductDto> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/products/{id}',
            path: {
                'id': id,
            },
        });
    }
    /**
     * @param id
     * @param requestBody
     * @returns ProductDto OK
     * @throws ApiError
     */
    public patchProduct(
        id: number,
        requestBody: ProductDto,
    ): CancelablePromise<ProductDto> {
        return this.httpRequest.request({
            method: 'PATCH',
            url: '/api/v1/products/{id}',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @param page
     * @param size
     * @param sort
     * @returns PageResponseDtoProductDto OK
     * @throws ApiError
     */
    public getAllProducts(
        page?: number,
        size: number = 20,
        sort: string = 'productId,asc',
    ): CancelablePromise<PageResponseDtoProductDto> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/products',
            query: {
                'page': page,
                'size': size,
                'sort': sort,
            },
        });
    }
    /**
     * @param requestBody
     * @returns ProductDto OK
     * @throws ApiError
     */
    public addProduct(
        requestBody: ProductDto,
    ): CancelablePromise<ProductDto> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/products',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @param requestBody
     * @returns string OK
     * @throws ApiError
     */
    public addMultipleProducts(
        requestBody: Array<ProductDto>,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/products/bulk',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * @param productName
     * @param page
     * @param size
     * @param sort
     * @returns PageResponseDtoProductDto OK
     * @throws ApiError
     */
    public searchProductsByNamePaged(
        productName: string,
        page?: number,
        size: number = 20,
        sort: string = 'productId,asc',
    ): CancelablePromise<PageResponseDtoProductDto> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/products/search',
            query: {
                'productName': productName,
                'page': page,
                'size': size,
                'sort': sort,
            },
        });
    }
    /**
     * @param category
     * @param page
     * @param size
     * @param sort
     * @returns PageResponseDtoProductDto OK
     * @throws ApiError
     */
    public getProductByCategoryPaged(
        category: string,
        page?: number,
        size: number = 20,
        sort: string = 'productId,asc',
    ): CancelablePromise<PageResponseDtoProductDto> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/products/category/{category}',
            path: {
                'category': category,
            },
            query: {
                'page': page,
                'size': size,
                'sort': sort,
            },
        });
    }
}
