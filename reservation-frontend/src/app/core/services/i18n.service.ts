import { Injectable, signal } from '@angular/core';
import { TRANSLATIONS } from '../i18n/translations';

export type Language = 'es' | 'en';

@Injectable({
  providedIn: 'root'
})
export class I18nService {
  private readonly STORAGE_KEY = 'hospentra_lang';
  private readonly defaultLang: Language = 'es';

  readonly currentLang = signal<Language>(this.getInitialLanguage());

  private getInitialLanguage(): Language {
    try {
      const saved = localStorage.getItem(this.STORAGE_KEY) as Language | null;
      if (saved === 'es' || saved === 'en') {
        return saved;
      }
    } catch {
      return this.defaultLang;
    }
    return this.defaultLang;
  }

  setLanguage(lang: Language): void {
    if (lang !== this.currentLang()) {
      this.currentLang.set(lang);
      try {
        localStorage.setItem(this.STORAGE_KEY, lang);
      } catch {}
    }
  }

  toggleLanguage(): void {
    this.setLanguage(this.currentLang() === 'es' ? 'en' : 'es');
  }

  t(key: string, params?: Record<string, string | number | null | undefined>): string {
    const lang = this.currentLang();
    const dictionary = TRANSLATIONS[lang] || TRANSLATIONS.es;
    const parts = key.split('.');
    let current: any = dictionary;

    for (const part of parts) {
      if (current && typeof current === 'object' && part in current) {
        current = current[part];
      } else {
        current = undefined;
        break;
      }
    }

    if (typeof current !== 'string') {
      let fallback: any = TRANSLATIONS.es;
      for (const part of parts) {
        if (fallback && typeof fallback === 'object' && part in fallback) {
          fallback = fallback[part];
        } else {
          fallback = undefined;
          break;
        }
      }
      current = typeof fallback === 'string' ? fallback : key;
    }

    let text = current as string;
    if (params) {
      Object.entries(params).forEach(([paramKey, paramVal]) => {
        text = text.replace(new RegExp(`\\{${paramKey}\\}`, 'g'), paramVal != null ? String(paramVal) : '');
      });
    }

    return text;
  }
}
