/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AdminPasswordUpdateRequest } from '../models/AdminPasswordUpdateRequest';
import type { UserDto } from '../models/UserDto';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class UsersService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * Update a user's password
     * @param userName
     * @param requestBody
     * @returns string OK
     * @throws ApiError
     */
    public updatePassword(
        userName: string,
        requestBody: AdminPasswordUpdateRequest,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'PATCH',
            url: '/api/v1/users/{userName}/password',
            path: {
                'userName': userName,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * List all users
     * @returns UserDto OK
     * @throws ApiError
     */
    public getAllUsers(): CancelablePromise<Array<UserDto>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/users',
        });
    }
    /**
     * Delete a user by username
     * Idempotent; returns 204 even if user already absent. Fails if attempting to remove last admin.
     * @param username
     * @returns void
     * @throws ApiError
     */
    public deleteUserByUsername(
        username: string,
    ): CancelablePromise<void> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/users/{username}',
            path: {
                'username': username,
            },
            errors: {
                401: `Unauthorized`,
                403: `Forbidden`,
                409: `Cannot delete last remaining admin`,
            },
        });
    }
}
