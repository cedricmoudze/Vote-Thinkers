// Fonctions pour ouvrir et fermer les modals
function openModal(modalId) {
    document.getElementById(modalId).style.display = 'block';
}

function closeModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
}

// Gestionnaires d'événements pour les boutons
document.getElementById('adminLoginBtn').addEventListener('click', () => openModal('loginModal'));
document.getElementById('createAccountLink').addEventListener('click', () => {
    closeModal('loginModal');
    openModal('createAccountModal');
});

if (document.getElementById('aboutBtn')) {
    document.getElementById('aboutBtn').addEventListener('click', () => openModal('aboutModal'));
}

if (document.getElementById('addCandidateBtn')) {
    document.getElementById('addCandidateBtn').addEventListener('click', () => openModal('candidateModal'));
}

// Fermer les modals quand on clique sur la croix
document.querySelectorAll('.close').forEach(closeBtn => {
    closeBtn.addEventListener('click', () => {
        closeBtn.closest('.modal').style.display = 'none';
    });
});

// Fermer les modals quand on clique en dehors
window.onclick = function(event) {
    if (event.target.classList.contains('modal')) {
        event.target.style.display = 'none';
    }
}

// Fonction pour créer dynamiquement les cartes des candidats
function createCandidateCard(candidate) {
    const card = document.createElement('div');
    card.className = 'candidate-card';
    card.innerHTML = `
        <img src="${candidate.photoUrl}" alt="${candidate.fullName}">
        <div class="candidate-info">
            <p>${candidate.voteNumber} - ${candidate.fullName}</p>
            <p>100 FCFA</p>
            <button class