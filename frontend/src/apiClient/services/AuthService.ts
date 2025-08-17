/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AuthRequest } from '../models/AuthRequest';
import type { AuthResponse } from '../models/AuthResponse';
import type { UserDto } from '../models/UserDto';
import type { UserRegistrationRequest } from '../models/UserRegistrationRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class AuthService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * Register a new user (ADMIN only)
     * @param requestBody
     * @returns UserDto User registered
     * @throws ApiError
     */
    public register(
        requestBody: UserRegistrationRequest,
    ): CancelablePromise<UserDto> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/auth/register',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Validation error`,
                401: `Unauthorized (no/invalid token)`,
                403: `Forbidden (not admin)`,
                409: `Username or email already exists`,
            },
        });
    }
    /**
     * Login with username & password
     * Returns JWT access & refresh tokens.
     * @param requestBody
     * @returns AuthResponse Login successful
     * @throws ApiError
     */
    public login(
        requestBody: AuthRequest,
    ): CancelablePromise<AuthResponse> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/auth/login',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Validation or bad request`,
                401: `Invalid credentials`,
            },
        });
    }
    /**
     * Forgot password placeholder
     * Returns instruction message.
     * @param username
     * @returns string Instruction returned
     * @throws ApiError
     */
    public forgotPassword(
        username: string,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/auth/forgot-password',
            query: {
                'username': username,
            },
        });
    }
}
