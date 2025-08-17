/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type UserRegistrationRequest = {
    username: string;
    userEmail: string;
    userPassword: string;
    userRole: UserRegistrationRequest.userRole;
    userContact: string;
    userSalary?: number;
};
export namespace UserRegistrationRequest {
    export enum userRole {
        ADMIN = 'ADMIN',
        USER = 'USER',
    }
}

