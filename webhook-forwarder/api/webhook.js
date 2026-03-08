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

            // Send the payload to Pusher
            await pusher.trigger('payos-webhook', 'payment', payload);

            console.log('Successfully forwarded payload to Pusher');
            res.status(200).json({ success: true, message: 'Forwarded to Pusher' });
        } catch (error) {
            console.error('Error forwarding to Pusher:', error);
            res.status(500).json({ success: false, error: 'Failed to forward to Pusher' });
        }
    } else {
        res.status(405).json({ success: false, message: 'Method not allowed' });
    }
};
