import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateHttpLoader, provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { BaseChartDirective, provideCharts, withDefaultRegisterables } from 'ng2-charts';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { NavBarComponent } from './components/nav-bar/nav-bar.component';
import { FooterComponent } from './components/footer/footer.component';
import { LoadingComponent } from './components/loading/loading.component';
import { PaginationComponent } from './components/pagination/pagination.component';
import { BreadcrumbComponent } from './components/breadcrumb/breadcrumb.component';
import { HomeComponent } from './components/home/home.component';
import { EmployeeSearchComponent } from './components/employee-search/employee-search.component';
import { PaieCalculComponent } from './components/paie-calcul/paie-calcul.component';
import { PaieDetailComponent } from './components/paie-detail/paie-detail.component';
import { PointageResultComponent } from './components/pointage-result/pointage-result.component';
import { PointageAjoutComponent } from './components/pointage-ajout/pointage-ajout.component';
import { PointageAnomaliesComponent } from './components/pointage-anomalies/pointage-anomalies.component';
import { PointageAnalyticsComponent } from './components/pointage-analytics/pointage-analytics.component';
import { GoToTopComponent } from './components/go-to-top/go-to-top.component';
import { FlatpickrDirective } from './directives/flatpickr.directive';
import { RequestInterceptor } from './config/RequestInterceptor.service';

@NgModule({
  declarations: [
    AppComponent,
    NavBarComponent,
    FooterComponent,
    LoadingComponent,
    PaginationComponent,
    BreadcrumbComponent,
    HomeComponent,
    EmployeeSearchComponent,
    PaieCalculComponent,
    PaieDetailComponent,
    PointageResultComponent,
    PointageAjoutComponent,
    PointageAnomaliesComponent,
    PointageAnalyticsComponent,
    GoToTopComponent,
    FlatpickrDirective
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useClass: TranslateHttpLoader
      }
    }),
    // Directive standalone (ng2-charts v6+) : importable directement dans un NgModule
    // sans wrapper module dédié, voir pointage-analytics.component.html.
    BaseChartDirective
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: RequestInterceptor,
      multi: true
    },
    provideTranslateHttpLoader({
      prefix: './assets/i18n/',
      suffix: '.json'
    }),
    provideCharts(withDefaultRegisterables())
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
