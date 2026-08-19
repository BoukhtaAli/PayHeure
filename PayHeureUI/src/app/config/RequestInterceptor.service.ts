import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpInterceptor,
  HttpHandler,
  HttpRequest,
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { LoadingService } from './loading.service';

/**
 * Affiche le spinner global pendant chaque appel API, quel que soit l'écran qui l'a déclenché.
 * Pas de gestion de session ici : l'application n'a pas d'authentification (voir README).
 */
@Injectable()
export class RequestInterceptor implements HttpInterceptor {

  constructor(private loadingService: LoadingService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    this.loadingService.setLoadingState(true);

    return next.handle(req).pipe(
      finalize(() => this.loadingService.setLoadingState(false))
    );
  }
}
