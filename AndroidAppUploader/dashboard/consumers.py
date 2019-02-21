from channels.generic.websocket import WebsocketConsumer
from asgiref.sync import async_to_sync
import json

class UploadConsumer(WebsocketConsumer):
    def connect(self):
        self.room_group_name = 'pool'

        async_to_sync(self.channel_layer.group_add) (
            self.room_group_name,
            self.channel_name
        )
        self.accept()

    def disconnect(self, code):
        async_to_sync(self.channel_layer.group_discard) (
            self.room_group_name,
            self.channel_name
        )

    def receive(self, text_data):
        print(text_data)
        text_data_json = json.loads(text_data)
        message = text_data_json['message']
        file_name = text_data_json['file_name']

        async_to_sync(self.channel_layer.group_send) (
            self.room_group_name,
            {
                'type': 'upload_message',
                'message': message,
                'file_name': file_name
            }
        )

    def upload_message(self, event):
        message = event['message']
        file_name = event['file_name']

        self.send(text_data=json.dumps({
            'message': message,
            'file_name': file_name
        }))