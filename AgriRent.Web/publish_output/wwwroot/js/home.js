function handleSearch(event) {
    event.preventDefault();
    if (window.agriRentHome && !window.agriRentHome.isLoggedIn) {
        window.location.href = window.agriRentHome.loginUrl;
        return false;
    }
    // Handle actual search when logged in (can redirect to a full search page if needed)
    return false;
}

document.addEventListener('DOMContentLoaded', function () {
    const searchInput = document.getElementById('searchKeyword');
    const searchLocation = document.getElementById('searchLocation');
    const searchCategory = document.getElementById('searchCategory');
    const searchResultsContainer = document.getElementById('searchResultsContainer');

    let searchTimeout;

    function performSearch() {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(async () => {
            const keyword = searchInput.value.trim();
            const location = searchLocation.value;
            const categoryId = searchCategory.value;

            if (!keyword && !location && !categoryId) {
                searchResultsContainer.classList.add('d-none');
                return;
            }

            try {
                const url = new URL(window.location.origin + '/api/search');
                if (keyword) url.searchParams.append('keyword', keyword);
                if (location) url.searchParams.append('location', location);
                if (categoryId) url.searchParams.append('categoryId', categoryId);
                
                const response = await fetch(url);
                if (response.ok) {
                    let result = await response.json();
                    if (result && result.data && result.data.length > 0) {
                        renderResults(result.data);
                    } else {
                        renderResults([]);
                    }
                } else {
                    renderResults([]);
                }
            } catch (error) {
                console.error('Search failed:', error);
                renderResults([]);
            }
        }, 300); // debounce 300ms
    }

    function renderResults(results) {
        if (!results || results.length === 0) {
            searchResultsContainer.innerHTML = '<div class="p-3 text-center text-muted">No results found</div>';
            searchResultsContainer.classList.remove('d-none');
            return;
        }

        let html = '<div class="list-group list-group-flush">';
        results.forEach(item => {
            const link = item.type === 'Equipment' ? '/Equipment' : '/Products';
            const img = item.imageUrl ? item.imageUrl : '/images/default-image.svg';
            const price = item.price ? `₹${item.price}/${item.priceType}` : 'Price on Request';
            const typeColor = item.type === 'Equipment' ? 'bg-success' : 'bg-warning text-dark';
            
            html += `
                <a href="${link}" class="list-group-item list-group-item-action d-flex align-items-center p-3">
                    <img src="${img}" alt="${item.name}" class="rounded me-3" style="width: 50px; height: 50px; object-fit: cover;" onerror="this.src='/images/default-image.svg'">
                    <div class="flex-grow-1">
                        <h6 class="mb-0 fw-bold">${item.name}</h6>
                        <small class="text-muted"><i class="fas fa-map-marker-alt me-1"></i>${item.location} • ${item.categoryName}</small>
                    </div>
                    <div class="text-end">
                        <span class="badge ${typeColor} mb-1">${item.type}</span><br>
                        <small class="fw-bold text-success">${price}</small>
                    </div>
                </a>
            `;
        });
        html += '</div>';
        
        searchResultsContainer.innerHTML = html;
        searchResultsContainer.classList.remove('d-none');
    }

    // Event listeners for dynamic search
    if (searchInput) searchInput.addEventListener('input', performSearch);
    if (searchLocation) searchLocation.addEventListener('change', performSearch);
    if (searchCategory) searchCategory.addEventListener('change', performSearch);

    // Hide results when clicking outside
    document.addEventListener('click', (e) => {
        if (!e.target.closest('.search-container')) {
            if (searchResultsContainer) {
                searchResultsContainer.classList.add('d-none');
            }
        }
    });
});
