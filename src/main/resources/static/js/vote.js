document.addEventListener('DOMContentLoaded', function() {
    const voteButtons = document.querySelectorAll('.vote-button');
    const modal = document.getElementById('paymentModal');
    const mobileMoneyBtn = document.getElementById('mobileMoney');
    const orangeMoneyBtn = document.getElementById('orangeMoney');
    const
    let currentCandidateId;

    voteButtons.forEach(button => {
        button.addEventListener('click', function() {
            currentCandidateId = this.getAttribute('data-candidate-id');
            modal.style.display = 'block';
        });
    });

    function vote(paymentMethod) {
        fetch(`/vote/${currentCandidateId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `paymentMethod=${paymentMethod}`
        }).then(() => {
            modal.style.display = 'none';
            location.reload(); // Recharge la page pour mettre à jour les votes
        });
    }

    mobileMoneyBtn.addEventListener('click', () => vote('mobileMoney'));
    orangeMoneyBtn.addEventListener('click', () => vote('orangeMoney'));

    window.onclick = function(event) {
        if (event.target == modal) {
            modal.style.display = 'none';
        }
    }
});
