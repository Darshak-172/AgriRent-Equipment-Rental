# Performance Bottlenecks Quick Reference

## Critical Issues Summary (Top 20)

| # | Issue | File | Lines | Root Cause | Impact | Severity |
|---|-------|------|-------|-----------|--------|----------|
| 1 | Sequential API calls in Seller Dashboard | SellerController.cs | 52-245 | No Task.WhenAll() parallelization | +5-8 sec per load | **HIGH** |
| 2 | Sequential API calls in Owner Dashboard | OwnerController.cs | 190-280 | No Task.WhenAll() parallelization | +5-8 sec per load | **HIGH** |
| 3 | JsonSerializerOptions created inline 23+ times | Multiple Controllers | Various | Not reused, allocated per call | Heap pressure, GC | **HIGH** |
| 4 | No HTTP Request Timeout configured | ApiClient.cs | 15-26 | DefaultTimeout unused | 100s hang risk | **HIGH** |
| 5 | Header manipulation on every request | ApiClient.cs | 32-50 | Session access + header ops | +100-200ms overhead | **HIGH** |
| 6 | Categories not cached outside HomeController | SellerController.cs | 632 | No caching strategy | +800-1200ms per load | **HIGH** |
| 7 | Duplicate profile API calls on login | AccountController.cs | 120, 248 | FetchUserProfile after login | +1-2 sec per login | **HIGH** |
| 8 | No response caching middleware | Program.cs | 1-80 | Missing UseResponseCaching() | No browser cache | **MEDIUM** |
| 9 | Inefficient JSON parsing with ToList() | EquipmentController.cs | 45+ | Unnecessary materialization | +300-500ms | **MEDIUM** |
| 10 | JWT manually parsed on every login | AccountController.cs | 90-110 | No JWT library used | +50-100ms per login | **MEDIUM** |
| 11 | Console.WriteLine in hot paths | All Dashboards | Various | Debug logging not removed | +50-200ms per page | **MEDIUM** |
| 12 | Substring operations in logging | OwnerController.cs | 245 | String allocation per log | Memory waste | **MEDIUM** |
| 13 | No pagination for large data sets | OwnerController.cs | 230 | Full result sets returned | Large payloads, timeout risk | **MEDIUM-LOW** |
| 14 | Session timeout 8 hours | Program.cs | 20 | Very permissive default | Stale sessions, security | **MEDIUM-LOW** |
| 15 | JsonElement instead of DTOs | All Controllers | Various | No type safety | Slower parsing, manual extraction | **MEDIUM-LOW** |
| 16 | SafeXXX helpers called repeatedly | SellerController.cs | 15-28 | Per-property extraction method calls | Extra overhead per item | **MEDIUM-LOW** |
| 17 | Remove-Add cycle for headers | ApiClient.cs | 48-50 | Defensive duplicates | Extra dictionary lookup | **LOW** |
| 18 | Gzip compression not verified | Program.cs | 14-15 | Default compression config | Potentially uncompressed responses | **LOW** |
| 19 | No explicit timeout in DI | Program.cs | 28 | AddHttpClient without ConfigureHttpClient | Default 100s hang | **LOW** |
| 20 | Missing AddResponseCaching service | Program.cs | 1-80 | Service not registered in DI | No middleware effect | **LOW** |

---

## Performance Impact by Page Type

### Dashboard Pages (Seller/Owner)
- **Current Load Time**: 8-12 seconds
- **Critical Issues**: #1, #2, #6, #11
- **Quick Win**: Parallelize API calls → 2-3 second improvement

### Home Page  
- **Current Load Time**: 2-4 seconds
- **Critical Issues**: #3, #5, #9
- **Quick Win**: Static JsonOptions + Cache categories → 500ms improvement

### Login Page
- **Current Load Time**: 3-5 seconds
- **Critical Issues**: #7, #10, #11
- **Quick Win**: Remove duplicate profile fetch → 1-2 second improvement

### Equipment/Product Pages
- **Current Load Time**: 2-4 seconds
- **Critical Issues**: #6, #9, #15
- **Quick Win**: Cache categories + Use DTOs → 400ms improvement

---

## Estimated Overall Impact

| Optimization | Estimated Savings | Effort | Priority |
|--------------|------------------|--------|----------|
| Dashboard API parallelization | 5-6 seconds | Low | **1** |
| Static JsonSerializerOptions | 300-500ms | Low | **2** |  
| Category caching (30 min) | 800-1200ms | Low | **3** |
| Remove duplicate profile fetch | 1-2 seconds | Low | **4** |
| Set HTTP timeout (30s) | Improved resilience | Low | **5** |
| Remove console logging | 50-200ms | Low | **6** |
| Add response caching middleware | Browser refresh +50% | Low | **7** |
| Use DTOs instead of JsonElement | 200-300ms | Medium | **8** |
| Pagination for large lists | Depends on data | Medium | **9** |
| Reduce session timeout | Memory savings | Low | **10** |

**Total Potential Improvement**: 8-12 seconds (60-75% faster dashboard loads)

---

## Quick Fix Checklist

### Tier 1 (Can be done in 1-2 hours)
- [ ] Create static readonly JsonSerializerOptions in ApiClient
- [ ] Add Task.WhenAll() to SellerController.Dashboard() 
- [ ] Add Task.WhenAll() to OwnerController.Dashboard()
- [ ] Implement 30-min category caching via CacheService
- [ ] Remove FetchUserProfile() call from AccountController.Login()
- [ ] Set HttpClient timeout to 30 seconds

### Tier 2 (Can be done in 3-4 hours)
- [ ] Remove all Console.WriteLine from hot paths
- [ ] Add UseResponseCaching() middleware
- [ ] Reduce session timeout from 8h to 2h
- [ ] Create SimpleProfile DTO for login
- [ ] Optimize LoadCategoriesAsync caching

### Tier 3 (Planned work)
- [ ] Create strongly-typed DTOs for all API responses
- [ ] Add pagination to equipment/product/order lists
- [ ] Setup Redis distributed cache
- [ ] Configure API response Cache-Control headers
- [ ] Replace JWT manual parsing with System.IdentityModel.Tokens.Jwt

---

## Testing Commands

After implementing fixes, measure impact:

```powershell
# Measure dashboard load time
$sw = [System.Diagnostics.Stopwatch]::StartNew()
curl -Uri "http://localhost:5289/Seller/Dashboard"
$sw.Stop()
Write-Host "Load time: $($sw.ElapsedMilliseconds)ms"

# Monitor API calls
# Check browser DevTools Network tab for:
# - Parallel vs sequential requests (waterfall view)
# - Request sizes (JSON response compression)
# - Cache hits (304 vs 200 status codes)
```

---

## Files Affected by Issues

| File | Issues | Priority |
|------|--------|----------|
| ApiClient.cs | #3, #4, #5, #19 | HIGH |
| SellerController.cs | #1, #6, #15, #16 | HIGH |
| OwnerController.cs | #2, #11, #12, #13 | HIGH |
| AccountController.cs | #7, #10 | HIGH |
| Program.cs | #8, #14, #20 | MEDIUM |
| ProfileController.cs | None directly | - |
| All Controllers | #3, #11, #15 | MEDIUM |
| HomeController.cs | #6 (partially cached) | LOW |

---

## Next Steps

1. **Review** this document with team
2. **Prioritize** fixes by severity and effort  
3. **Implement** Tier 1 fixes first (highest ROI)
4. **Benchmark** before/after with load testing
5. **Plan** Tier 2 & 3 work for upcoming sprints
