const Pusher = require("pusher");

const pusher = new Pusher({
    appId: process.env.PUSHER_APP_ID,
    key: process.env.PUSHER_KEY,
    secret: process.env.PUSHER_SECRET,
    cluster: process.env.PUSHER_CLUSTER,
    useTLS: true
});

module.exports = async (req, res) => {
    if (req.method === 'POST') {
        try {
            const payload = req.body;
            // Secure-Token is often used in Casso V2
            const secureToken = req.headers['secure-token'] || req.headers['x-casso-signature'];

            console.log('Received Casso Webhook, Token present:', !!secureToken);

            // Send the payload to Pusher
            await pusher.trigger('casso-webhook', 'transaction', payload);

            console.log('Successfully forwarded Casso payload to Pusher');
            res.status(200).json({ error: 0, message: 'Ok' });
        } catch (error) {
            console.error('Error forwarding to Pusher:', error);
            res.status(500).json({ error: 1, message: 'Failed to forward to Pusher' });
        }
    } else {
        res.status(405).json({ error: 1, message: 'Method not allowed' });
    }
};
