import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { RegisterRequest } from '../models/register-request';
import { AuthenticationResponse } from '../models/Authentication-response';

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {
  private baseUrl : string = '/http:localhost:8080/auth';
  constructor(private http:HttpClient) { }
  
  register(registerRequest:RegisterRequest){
    return this.http.post<AuthenticationResponse>(`${this.baseUrl}/register`,registerRequest);
  }


}
