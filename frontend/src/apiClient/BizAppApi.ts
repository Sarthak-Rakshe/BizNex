/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BaseHttpRequest } from './core/BaseHttpRequest';
import type { OpenAPIConfig } from './core/OpenAPI';
import { AxiosHttpRequest } from './core/AxiosHttpRequest';
import { AuthService } from './services/AuthService';
import { BillingControllerService } from './services/BillingControllerService';
import { CustomerControllerService } from './services/CustomerControllerService';
import { ProductControllerService } from './services/ProductControllerService';
import { UsersService } from './services/UsersService';
type HttpRequestConstructor = new (config: OpenAPIConfig) => BaseHttpRequest;
export class BizAppApi {
    public readonly auth: AuthService;
    public readonly billingController: BillingControllerService;
    public readonly customerController: CustomerControllerService;
    public readonly productController: ProductControllerService;
    public readonly users: UsersService;
    public readonly request: BaseHttpRequest;
    constructor(config?: Partial<OpenAPIConfig>, HttpRequest: HttpRequestConstructor = AxiosHttpRequest) {
        this.request = new HttpRequest({
            BASE: config?.BASE ?? 'http://localhost:8081',
            VERSION: config?.VERSION ?? '1.1.0',
            WITH_CREDENTIALS: config?.WITH_CREDENTIALS ?? false,
            CREDENTIALS: config?.CREDENTIALS ?? 'include',
            TOKEN: config?.TOKEN,
            USERNAME: config?.USERNAME,
            PASSWORD: config?.PASSWORD,
            HEADERS: config?.HEADERS,
            ENCODE_PATH: config?.ENCODE_PATH,
        });
        this.auth = new AuthService(this.request);
        this.billingController = new BillingControllerService(this.request);
        this.customerController = new CustomerControllerService(this.request);
        this.productController = new ProductControllerService(this.request);
        this.users = new UsersService(this.request);
    }
}

