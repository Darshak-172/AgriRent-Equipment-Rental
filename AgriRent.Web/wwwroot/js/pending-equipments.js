// pending-equipments.js

document.addEventListener("DOMContentLoaded", function () {
    // Search Functionality
    const searchInput = document.getElementById("searchInput");
    const cards = document.querySelectorAll(".equipment-table-row");

    if (searchInput && cards) {
        searchInput.addEventListener("input", function () {
            const val = this.value.toLowerCase();
            cards.forEach(card => {
                const name = card.getAttribute("data-name");
                if (name && name.includes(val)) {
                    card.style.display = ""; // Table row default display
                } else {
                    card.style.display = "none";
                }
            });
        });
    }
});

// Modal Variables
const modal = document.getElementById("detailsModal");
const overlay = document.getElementById("modalOverlay");

function openModal(btn) {
    if (!modal || !overlay) return;

    // Populate data
    document.getElementById("modalImg").src = btn.dataset.img;
    document.getElementById("modalName").textContent = btn.dataset.name;
    document.getElementById("modalDesc").textContent = btn.dataset.desc || "No description provided.";
    document.getElementById("modalPrice").textContent = btn.dataset.price;
    document.getElementById("modalType").textContent = "/ " + btn.dataset.type;
    document.getElementById("modalLoc").textContent = btn.dataset.loc;

    // Status Badge Styling
    const statusEl = document.getElementById("modalStatus");
    statusEl.className = "modal-status-badge " + (btn.dataset.status ? btn.dataset.status.toLowerCase() : "pending");
    statusEl.innerHTML = `<i class="fa-solid fa-circle"></i> ` + btn.dataset.status;

    document.getElementById("modalOwner").textContent = btn.dataset.owner;
    document.getElementById("modalMobile").textContent = btn.dataset.mobile;

    // Avatar logic
    const ownerName = btn.dataset.owner;
    const initial = ownerName && ownerName.trim().length > 0 ? ownerName.trim().charAt(0).toUpperCase() : "?";
    document.getElementById("modalOwnerAvatar").textContent = initial;

    // WhatsApp logic
    const waLink = document.getElementById("modalWhatsapp");
    if (btn.dataset.mobile) {
        waLink.href = 'https://wa.me/' + btn.dataset.mobile.replace(/[^0-9\+]/g, '');
        waLink.style.display = 'flex';
    } else {
        waLink.style.display = 'none';
    }

    // Show Modal
    modal.classList.add("open");
    overlay.classList.add("open");
    document.body.style.overflow = "hidden"; // Prevent background scroll
}

function closeModal() {
    if (!modal || !overlay) return;
    modal.classList.remove("open");
    overlay.classList.remove("open");
    document.body.style.overflow = "";
}
