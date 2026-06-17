import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { environment } from '../../../environments/environment';
import { SidebarComponent } from '../../components/sidebar/sidebar.component';

// ── Types ──────────────────────────────────────────────────────
export interface FilterCondition {
  param:     string;
  column:    string;
  operator:  string;
  label:     string;
  inputType: string;
  options:   string[];
  default:   string;
  required:  boolean;
}

export interface TableColumn {
  name: string;
  type: string;
}

export interface ApiConfig {
  id?:           string;
  aliasName:     string;
  configType:    string;
  schemaName:    string;
  tableName:     string;
  displayType:   string;
  configuration: string;
  projectId?:    string;
  isActive?:     boolean;
  createdAt?:    string;
}

@Component({
  selector: 'app-api-json-generator',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent],
  templateUrl: './api-json-generator.component.html',
  styleUrl: './api-json-generator.component.scss'
})
export class ApiJsonGeneratorComponent implements OnInit {

  projectId = '';

  // ── View state ──────────────────────────────────────────────
  view: 'list' | 'form' = 'list';
  editId: string | null  = null;
  editMode               = false;

  // ── Toast ───────────────────────────────────────────────────
  toast: { msg: string; type: 'success' | 'error' | 'info' } | null = null;

  // ── List state ──────────────────────────────────────────────
  configs:      ApiConfig[] = [];
  filteredList: ApiConfig[] = [];
  pagedList:    ApiConfig[] = [];
  isLoading     = false;
  searchText    = '';
  currentPage   = 1;
  pageSize      = 10;
  totalPages    = 1;

  // ── Form: basic ─────────────────────────────────────────────
  aliasName    = '';
  configType   = 'form_crud';
  displayType  = 'form';
  allowedRoles = 'default';

  // ── Form: data source ────────────────────────────────────────
  schema      = '';
  tableName   = '';
  primaryKey  = 'id';
  sourceMode: 'table' | 'filter' | 'query' = 'table';
  sqlQuery    = '';

  // ── Columns (fetched from PostgREST) ─────────────────────────
  columns:       TableColumn[] = [];
  fetchingCols   = false;
  colFetchStatus = '';
  colFetchOk     = false;

  // ── Dynamic WHERE filters ────────────────────────────────────
  filterLogic: 'AND' | 'OR' = 'AND';
  filters: FilterCondition[] = [];

  sqlFilterLogic: 'AND' | 'OR' = 'AND';
  sqlFilters: FilterCondition[] = [];

  readonly OPERATORS = ['=', '!=', '>', '>=', '<', '<=', 'LIKE', 'NOT LIKE', 'IN', 'NOT IN', 'IS NULL', 'IS NOT NULL'];
  readonly INPUT_TYPES = ['text', 'number', 'date', 'select', 'boolean'];

  // ── Config types ─────────────────────────────────────────────
  readonly CONFIG_TYPES = [
    { value: 'form_crud',  label: 'Form CRUD',   icon: 'bi-ui-checks' },
    { value: 'grid',       label: 'Grid',         icon: 'bi-table' },
    { value: 'chart',      label: 'Chart',        icon: 'bi-bar-chart-line' },
    { value: 'custom_sql', label: 'Custom SQL',   icon: 'bi-code-slash' }
  ];

  readonly DISPLAY_TYPES: Record<string, string[]> = {
    form_crud:  ['form'],
    grid:       ['grid'],
    chart:      ['barChart', 'lineChart', 'pieChart', 'donutChart'],
    custom_sql: ['grid', 'form', 'barChart']
  };

  // ── JSON preview ─────────────────────────────────────────────
  isSaving = false;

  private apiUrl = environment.apiUrl;

  constructor(
    private http:  HttpClient,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.projectId = this.route.snapshot.params['id'] || '';
    this.loadConfigs();
  }

  // ── Toast ────────────────────────────────────────────────────
  showToast(msg: string, type: 'success' | 'error' | 'info' = 'info'): void {
    this.toast = { msg, type };
    setTimeout(() => this.toast = null, 3000);
  }

  // ── Load configs ─────────────────────────────────────────────
  loadConfigs(): void {
    this.isLoading = true;
    let url = `${this.apiUrl}/api-configs`;
    if (this.projectId) url += `?project_id=${this.projectId}`;

    this.http.get<ApiConfig[]>(url).subscribe({
      next: res => {
        this.configs   = res || [];
        this.isLoading = false;
        this.applyFilter();
      },
      error: () => {
        this.isLoading = false;
        this.showToast('Failed to load configurations', 'error');
      }
    });
  }

  // ── Search / filter ──────────────────────────────────────────
  applyFilter(): void {
    const s = this.searchText.toLowerCase().trim();
    this.filteredList = s
      ? this.configs.filter(c =>
          (c.aliasName   || '').toLowerCase().includes(s) ||
          (c.configType  || '').toLowerCase().includes(s) ||
          (c.tableName   || '').toLowerCase().includes(s)
        )
      : [...this.configs];
    this.currentPage = 1;
    this.updatePagination();
  }

  updatePagination(): void {
    this.totalPages = Math.ceil(this.filteredList.length / this.pageSize) || 1;
    const start = (this.currentPage - 1) * this.pageSize;
    this.pagedList = this.filteredList.slice(start, start + this.pageSize);
  }

  goToPage(p: number)  { if (p >= 1 && p <= this.totalPages) { this.currentPage = p; this.updatePagination(); } }
  prevPage()            { this.goToPage(this.currentPage - 1); }
  nextPage()            { this.goToPage(this.currentPage + 1); }
  onPageSizeChange()    { this.currentPage = 1; this.updatePagination(); }

  get pageNumbers(): number[] {
    const max = 5;
    let s = Math.max(1, this.currentPage - Math.floor(max / 2));
    let e = Math.min(this.totalPages, s + max - 1);
    if (e - s + 1 < max) s = Math.max(1, e - max + 1);
    return Array.from({ length: e - s + 1 }, (_, i) => s + i);
  }

  // ── Fetch columns from Spring Boot → information_schema ──────
  fetchColumns(): void {
    if (!this.schema || !this.tableName) {
      this.showToast('Enter schema and table name first', 'error'); return;
    }
    this.fetchingCols   = true;
    this.colFetchStatus = '';
    this.colFetchOk     = false;

    this.http.get<TableColumn[]>(
      `${this.apiUrl}/schema/columns?schema=${encodeURIComponent(this.schema)}&table=${encodeURIComponent(this.tableName)}${this.projectId ? "&projectId=" + this.projectId : ""}`
    ).subscribe({
      next: cols => {
        if (cols && cols.length > 0) {
          this.columns        = cols;
          this.colFetchOk     = true;
          this.colFetchStatus = `${cols.length} columns loaded from ${this.schema}.${this.tableName}`;
        } else {
          this.colFetchStatus = `Table ${this.schema}.${this.tableName} not found or has no columns`;
          this.colFetchOk     = false;
        }
        this.fetchingCols = false;
      },
      error: err => {
        this.fetchingCols   = false;
        this.colFetchOk     = false;
        this.colFetchStatus = `Failed to fetch columns — check schema and table name`;
        this.showToast('Could not fetch columns', 'error');
        console.error(err);
      }
    });
  }

  // ── Filter management ─────────────────────────────────────────
  refreshFilterDropdowns(): void { /* triggers change detection */ }

  addFilter(isSql = false): void {
    const col  = this.columns[0]?.name || '';
    const cond: FilterCondition = {
      param: col + 'Filter', column: col, operator: '=',
      label: col, inputType: 'text', options: [], default: '', required: false
    };
    isSql ? this.sqlFilters.push(cond) : this.filters.push(cond);
  }

  removeFilter(i: number, isSql = false): void {
    isSql ? this.sqlFilters.splice(i, 1) : this.filters.splice(i, 1);
  }

  needsValue(op: string): boolean {
    return op !== 'IS NULL' && op !== 'IS NOT NULL';
  }

  // ── Config type change ────────────────────────────────────────
  onConfigTypeChange(): void {
    const types = this.DISPLAY_TYPES[this.configType] || ['form'];
    this.displayType = types[0];
  }

  get displayTypesForCurrent(): string[] {
    return this.DISPLAY_TYPES[this.configType] || ['form'];
  }

  // ── JSON builder ─────────────────────────────────────────────
  buildJSON(): any {
    const roles = this.allowedRoles.split(',').map(r => r.trim()).filter(Boolean);

    const ds: any = {
      type:       'form_crud',
      schema:     this.schema.trim(),
      table:      this.tableName.trim(),
      primaryKey: this.primaryKey.trim() || 'id'
    };

    if (this.sourceMode === 'filter' && this.filters.length) {
      ds.where = {
        logic: this.filterLogic,
        conditions: this.filters
          .filter(f => f.column)
          .map(f => {
            const c: any = { column: f.column, operator: f.operator };
            if (this.needsValue(f.operator)) c.value = f.default || '';
            return c;
          })
      };
    }

    if (this.sourceMode === 'filter' && this.filters.length) {
      ds.dynamicWhere = {
        logic:   this.filterLogic,
        filters: this.filters.filter(f => f.column).map(f => ({ ...f }))
      };
    }

    if (this.sourceMode === 'query') {
      ds.query = this.sqlQuery.trim();
      if (this.sqlFilters.length) {
        ds.dynamicWhere = {
          logic:   this.sqlFilterLogic,
          filters: this.sqlFilters.filter(f => f.column).map(f => ({ ...f }))
        };
      }
    }

    const json: any = {
      name:    this.aliasName.trim() || 'Configuration',
      display: { type: this.displayType },
      dataSource: ds,
      allowedRoles: roles.length ? roles : ['default']
    };

    if (this.columns.length) {
      json.columns = this.columns.map(c => ({ name: c.name, type: c.type }));
    }

    return json;
  }

  get configJSONString(): string {
    return JSON.stringify(this.buildJSON(), null, 2);
  }

  // ── Copy JSON ────────────────────────────────────────────────
  copyJSON(): void {
    navigator.clipboard.writeText(this.configJSONString).then(
      ()  => this.showToast('Copied to clipboard!', 'success'),
      ()  => this.showToast('Failed to copy', 'error')
    );
  }

  // ── Save ─────────────────────────────────────────────────────
  save(): void {
    if (!this.aliasName.trim()) { this.showToast('Name is required', 'error'); return; }
    if (!this.schema.trim())    { this.showToast('Schema is required', 'error'); return; }
    if (!this.tableName.trim()) { this.showToast('Table name is required', 'error'); return; }
    if (this.sourceMode === 'query' && !this.sqlQuery.trim()) {
      this.showToast('SQL query is required', 'error'); return;
    }

    this.isSaving = true;

    const body = {
      aliasName:     this.aliasName.trim(),
      configType:    this.configType,
      schemaName:    this.schema.trim(),
      tableName:     this.tableName.trim(),
      displayType:   this.displayType,
      configuration: this.configJSONString,
      projectId:     this.projectId || null,
      isActive:      true
    };

    const req = this.editMode && this.editId
      ? this.http.put(`${this.apiUrl}/api-configs/${this.editId}`, body)
      : this.http.post(`${this.apiUrl}/api-configs`, body);

    req.subscribe({
      next: () => {
        this.isSaving = false;
        this.showToast(this.editMode ? 'Updated!' : 'Saved!', 'success');
        this.resetForm();
        this.view = 'list';
        this.loadConfigs();
      },
      error: () => {
        this.isSaving = false;
        this.showToast('Failed to save', 'error');
      }
    });
  }

  // ── Edit ─────────────────────────────────────────────────────
  loadForEdit(c: ApiConfig): void {
    this.editMode = true;
    this.editId   = c.id || null;
    try {
      const cfg = typeof c.configuration === 'string'
        ? JSON.parse(c.configuration) : c.configuration;

      this.aliasName   = cfg.name         || c.aliasName || '';
      this.configType  = c.configType     || 'form_crud';
      this.displayType = cfg.display?.type || c.displayType || 'form';
      this.allowedRoles= (cfg.allowedRoles || ['default']).join(', ');
      this.schema      = cfg.dataSource?.schema     || c.schemaName || '';
      this.tableName   = cfg.dataSource?.table      || c.tableName  || '';
      this.primaryKey  = cfg.dataSource?.primaryKey || 'id';
      this.sqlQuery    = cfg.dataSource?.query      || '';
      this.columns     = cfg.columns                || [];

      if (cfg.dataSource?.query) {
        this.sourceMode   = 'query';
        this.sqlFilters   = cfg.dataSource?.dynamicWhere?.filters || [];
        this.sqlFilterLogic = cfg.dataSource?.dynamicWhere?.logic || 'AND';
      } else if (cfg.dataSource?.dynamicWhere || cfg.dataSource?.where) {
        this.sourceMode  = 'filter';
        this.filters     = cfg.dataSource?.dynamicWhere?.filters || [];
        this.filterLogic = cfg.dataSource?.dynamicWhere?.logic   || 'AND';
      } else {
        this.sourceMode = 'table';
      }

      this.view = 'form';
    } catch {
      this.showToast('Failed to parse configuration', 'error');
    }
  }

  // ── Delete ────────────────────────────────────────────────────
  deleteConfig(c: ApiConfig): void {
    if (!confirm(`Delete "${c.aliasName}"?`)) return;
    this.http.delete(`${this.apiUrl}/api-configs/${c.id}`).subscribe({
      next:  () => { this.showToast('Deleted', 'success'); this.loadConfigs(); },
      error: () => this.showToast('Failed to delete', 'error')
    });
  }

  // ── Reset ────────────────────────────────────────────────────
  resetForm(): void {
    this.aliasName    = '';
    this.configType   = 'form_crud';
    this.displayType  = 'form';
    this.allowedRoles = 'default';
    this.schema       = '';
    this.tableName    = '';
    this.primaryKey   = 'id';
    this.sourceMode   = 'table';
    this.sqlQuery     = '';
    this.columns      = [];
    this.filters      = [];
    this.sqlFilters   = [];
    this.filterLogic  = 'AND';
    this.sqlFilterLogic = 'AND';
    this.colFetchStatus = '';
    this.colFetchOk   = false;
    this.editMode     = false;
    this.editId       = null;
  }

  openCreate(): void { this.resetForm(); this.view = 'form'; }
  goBack():     void { this.resetForm(); this.view = 'list'; }

  // ── Helpers ───────────────────────────────────────────────────
  configTypeLabel(type: string): string {
    return this.CONFIG_TYPES.find(t => t.value === type)?.label || type;
  }
  configTypeIcon(type: string): string {
    return this.CONFIG_TYPES.find(t => t.value === type)?.icon || 'bi-braces';
  }
}